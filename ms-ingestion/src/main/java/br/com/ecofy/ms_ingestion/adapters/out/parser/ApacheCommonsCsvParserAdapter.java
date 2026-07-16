package br.com.ecofy.ms_ingestion.adapters.out.parser;

import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportErrorType;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import br.com.ecofy.ms_ingestion.core.port.out.ParseCsvPort;
import br.com.ecofy.ms_ingestion.core.port.out.ParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser CSV robusto (sem dependência externa — Apache Commons CSV não foi
 * adicionada por indisponibilidade offline; ver README/relatório).
 *
 * Regras:
 * - delimitador ';' com suporte a campos entre aspas duplas (que podem conter ';' e '""' escapado);
 * - detecção de cabeçalho (primeira linha contendo "date");
 * - formato: date;description;amount;currency  (currency opcional, default BRL);
 * - cada linha inválida vira um ImportError (rastreável) — NÃO derruba o job;
 * - linhas em branco são ignoradas.
 */
@Slf4j
@Component
public class ApacheCommonsCsvParserAdapter implements ParseCsvPort {

    private static final String DEFAULT_CURRENCY = "BRL";
    private static final char DELIMITER = ';';

    @Override
    public ParseResult parse(ImportJob job, String csvContent) {
        log.info("[ApacheCommonsCsvParserAdapter] - [parse] -> Parsing CSV para jobId={}", job.id());

        List<RawTransaction> transactions = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();

        if (csvContent == null || csvContent.isBlank()) {
            // Arquivo estruturalmente vazio -> deixa o service decidir (0 tx / 0 erros).
            log.warn("[ApacheCommonsCsvParserAdapter] - [parse] -> Conteúdo CSV vazio jobId={}", job.id());
            return ParseResult.of(transactions, errors);
        }

        String[] lines = csvContent.split("\\R", -1);
        int lineNo = 0;

        for (String line : lines) {
            lineNo++;

            if (line == null || line.isBlank()) {
                continue;
            }

            // Cabeçalho: primeira linha não-vazia contendo "date".
            if (lineNo == 1 && line.toLowerCase().contains("date")) {
                continue;
            }

            try {
                RawTransaction tx = parseLine(job, line);
                transactions.add(tx);
            } catch (LineParseException e) {
                log.debug("[ApacheCommonsCsvParserAdapter] - [parse] -> Linha inválida line={} reason={}", lineNo, e.getMessage());
                errors.add(ImportError.create(
                        job.id(),
                        lineNo,
                        line,
                        e.errorType,
                        e.getMessage()
                ));
            } catch (Exception e) {
                errors.add(ImportError.create(
                        job.id(),
                        lineNo,
                        line,
                        ImportErrorType.PARSE_ERROR,
                        "Unexpected error parsing line: " + e.getMessage()
                ));
            }
        }

        log.info("[ApacheCommonsCsvParserAdapter] - [parse] -> CSV parseado jobId={} validTx={} errors={}",
                job.id(), transactions.size(), errors.size());

        return ParseResult.of(transactions, errors);
    }

    // Faz o parse de uma única linha, lançando LineParseException (rastreável) quando inválida.
    private RawTransaction parseLine(ImportJob job, String line) {
        List<String> fields = splitCsv(line);

        if (fields.size() < 3) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR,
                    "Expected at least 3 fields (date;description;amount) but got " + fields.size());
        }

        String dateRaw = fields.get(0).trim();
        String desc = fields.get(1).trim();
        String amountRaw = fields.get(2).trim();
        String currency = fields.size() >= 4 && !fields.get(3).isBlank()
                ? fields.get(3).trim().toUpperCase()
                : DEFAULT_CURRENCY;

        if (dateRaw.isEmpty()) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR, "date is required");
        }
        if (amountRaw.isEmpty()) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR, "amount is required");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateRaw);
        } catch (DateTimeParseException e) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR,
                    "invalid date '" + dateRaw + "' (expected yyyy-MM-dd)");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountRaw.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR,
                    "invalid amount '" + amountRaw + "'");
        }

        if (currency.length() != 3) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR,
                    "invalid currency '" + currency + "' (expected 3-letter code)");
        }

        return RawTransaction.create(
                job.id(),
                null,
                desc,
                new TransactionDate(date),
                new Money(amount, currency),
                TransactionSourceType.FILE_CSV
        );
    }

    /**
     * Split de CSV com suporte a aspas duplas: campos entre aspas podem conter o
     * delimitador e aspas escapadas ("").
     */
    private static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"'); // aspas escapada ""
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == DELIMITER) {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }

    // Exceção interna para diferenciar erro de linha (rastreável) com o tipo apropriado.
    private static final class LineParseException extends RuntimeException {
        private final ImportErrorType errorType;

        LineParseException(ImportErrorType errorType, String message) {
            super(message);
            this.errorType = errorType;
        }
    }
}
