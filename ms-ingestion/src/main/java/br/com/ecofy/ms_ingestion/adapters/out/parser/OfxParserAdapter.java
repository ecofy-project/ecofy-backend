package br.com.ecofy.ms_ingestion.adapters.out.parser;

import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.EmptyFileException;
import br.com.ecofy.ms_ingestion.core.application.exception.FileLineLimitExceededException;
import br.com.ecofy.ms_ingestion.core.application.exception.FileLineTooLongException;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportErrorType;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import br.com.ecofy.ms_ingestion.core.port.out.ImportRecordHandler;
import br.com.ecofy.ms_ingestion.core.port.out.ParseOfxPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// Lê o OFX em streaming reconhecendo cada bloco de transação linha a linha, sem carregar o arquivo em memória.
@Slf4j
@Component
public class OfxParserAdapter implements ParseOfxPort {

    private static final String OPEN_TAG = "<STMTTRN>";
    private static final String CLOSE_TAG = "</STMTTRN>";

    // Limita as linhas por bloco para que uma tag nunca fechada não faça o bloco crescer indefinidamente.
    private static final int MAX_LINES_PER_BLOCK = 100;

    private final IngestionProperties properties;

    public OfxParserAdapter(IngestionProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public void parse(ImportJob job, java.io.Reader reader, ImportRecordHandler handler) {
        Objects.requireNonNull(job, "job must not be null");
        Objects.requireNonNull(reader, "reader must not be null");
        Objects.requireNonNull(handler, "handler must not be null");

        IngestionProperties.Upload limits = properties.getUpload();
        BoundedLineReader lines = new BoundedLineReader(reader, limits.getMaxLineLength());

        String curDef = null;
        Map<String, String> block = null;
        int blockLines = 0;
        long blockIndex = 0;
        long emitted = 0;

        String line;
        while ((line = lines.readLine()) != null) {
            if (!handler.continueProcessing()) {
                log.info("[OfxParserAdapter] - [parse] -> Parse interrompido pelo handler jobId={} blocos={}",
                        job.id(), blockIndex);
                return;
            }

            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (block == null) {
                // Moeda default do arquivo, válida para todos os blocos seguintes.
                if (curDef == null) {
                    String value = tagValue(trimmed, "CURDEF");
                    if (value != null) {
                        curDef = value;
                    }
                }
                if (containsIgnoreCase(trimmed, OPEN_TAG)) {
                    block = new HashMap<>();
                    blockLines = 0;
                    blockIndex++;
                    if (blockIndex > limits.getMaxLines()) {
                        throw new FileLineLimitExceededException(limits.getMaxLines());
                    }
                }
                continue;
            }

            if (containsIgnoreCase(trimmed, CLOSE_TAG)) {
                try {
                    handler.onValid(toTransaction(job, block, curDef));
                    emitted++;
                } catch (BlockParseException e) {
                    handler.onInvalid(ImportError.create(
                            job.id(), (int) blockIndex, null, e.errorType, e.getMessage()));
                } catch (RuntimeException e) {
                    handler.onInvalid(ImportError.create(
                            job.id(), (int) blockIndex, null, ImportErrorType.PARSE_ERROR,
                            "Unexpected error parsing STMTTRN block"));
                }
                block = null;
                continue;
            }

            if (++blockLines > MAX_LINES_PER_BLOCK) {
                throw new FileLineTooLongException(lines.lineNumber(), limits.getMaxLineLength());
            }

            collectTags(trimmed, block);
        }

        if (blockIndex == 0) {
            throw new EmptyFileException("noStmtTrnBlocks=true");
        }

        log.info("[OfxParserAdapter] - [parse] -> OFX parseado jobId={} blocos={} validos={}",
                job.id(), blockIndex, emitted);
    }

    private RawTransaction toTransaction(ImportJob job, Map<String, String> block, String curDef) {
        String trnAmtRaw = block.get("TRNAMT");
        String dtPostedRaw = block.get("DTPOSTED");

        if (trnAmtRaw == null || dtPostedRaw == null) {
            throw new BlockParseException(ImportErrorType.VALIDATION_ERROR,
                    "STMTTRN missing TRNAMT/DTPOSTED");
        }

        BigDecimal amountValue;
        try {
            amountValue = new BigDecimal(trnAmtRaw.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new BlockParseException(ImportErrorType.VALIDATION_ERROR, "invalid TRNAMT");
        }

        if (amountValue.precision() - amountValue.scale() > 15 || amountValue.scale() > 4) {
            throw new BlockParseException(ImportErrorType.VALIDATION_ERROR,
                    "TRNAMT out of supported range");
        }

        Instant postedAt = parseOfxToInstant(dtPostedRaw);
        LocalDate postedDateUtc = postedAt.atZone(ZoneOffset.UTC).toLocalDate();

        Money amount = (curDef == null)
                ? Money.of(amountValue)
                : Money.of(amountValue, curDef);

        return RawTransaction.create(
                job.id(),
                block.get("FITID"),
                resolveDescription(block.get("NAME"), block.get("MEMO")),
                TransactionDate.of(postedDateUtc),
                amount,
                TransactionSourceType.FILE_OFX
        );
    }

    // Coleta da linha apenas as tags de interesse, ignorando o restante do OFX.
    private static void collectTags(String line, Map<String, String> block) {
        for (String tag : new String[]{"TRNAMT", "DTPOSTED", "FITID", "NAME", "MEMO"}) {
            if (block.containsKey(tag)) {
                continue;
            }
            String value = tagValue(line, tag);
            if (value != null) {
                block.put(tag, value);
            }
        }
    }

    // Extrai o valor de uma tag na linha corrente, tolerando tags sem fechamento.
    private static String tagValue(String line, String tag) {
        String open = "<" + tag + ">";
        int start = indexOfIgnoreCase(line, open);
        if (start < 0) {
            return null;
        }
        int valueStart = start + open.length();

        int end = indexOfIgnoreCase(line, "</" + tag + ">");
        if (end < valueStart) {
            // Sem fechamento: valor vai até a próxima tag ou até o fim da linha.
            end = line.indexOf('<', valueStart);
            if (end < 0) {
                end = line.length();
            }
        }

        String value = line.substring(valueStart, end).trim();
        return value.isEmpty() ? null : value;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return indexOfIgnoreCase(haystack, needle) >= 0;
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return haystack.toUpperCase(Locale.ROOT).indexOf(needle.toUpperCase(Locale.ROOT));
    }

    // Define a melhor descrição para a transação priorizando NAME e fallback para MEMO/UNKNOWN.
    private static String resolveDescription(String name, String memo) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        if (memo != null && !memo.isBlank()) {
            return memo.trim();
        }
        return "UNKNOWN";
    }

    // Converte o DTPOSTED do OFX (com possíveis timezones/metadados) para Instant em UTC.
    private static Instant parseOfxToInstant(String raw) {
        String digitsOnly = raw.replaceAll("[^0-9]", "");

        try {
            if (digitsOnly.length() >= 14) {
                String ts = digitsOnly.substring(0, 14);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
                return LocalDateTime.parse(ts, fmt).toInstant(ZoneOffset.UTC);
            }
            if (digitsOnly.length() >= 8) {
                String dt = digitsOnly.substring(0, 8);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
                return LocalDate.parse(dt, fmt).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        } catch (RuntimeException e) {
            throw new BlockParseException(ImportErrorType.VALIDATION_ERROR, "invalid DTPOSTED");
        }
        throw new BlockParseException(ImportErrorType.VALIDATION_ERROR, "invalid DTPOSTED");
    }

    // Exceção interna para diferenciar erro de bloco (rastreável) com o tipo apropriado.
    private static final class BlockParseException extends RuntimeException {
        private final ImportErrorType errorType;

        BlockParseException(ImportErrorType errorType, String message) {
            super(message);
            this.errorType = errorType;
        }
    }
}
