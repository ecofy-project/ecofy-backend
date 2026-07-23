package br.com.ecofy.ms_ingestion.adapters.out.parser;

import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.EmptyFileException;
import br.com.ecofy.ms_ingestion.core.application.exception.ErrorDetail;
import br.com.ecofy.ms_ingestion.core.application.exception.FileColumnLimitExceededException;
import br.com.ecofy.ms_ingestion.core.application.exception.FileLineLimitExceededException;
import br.com.ecofy.ms_ingestion.core.application.exception.InvalidFileHeaderException;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportErrorType;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import br.com.ecofy.ms_ingestion.core.port.out.ImportRecordHandler;
import br.com.ecofy.ms_ingestion.core.port.out.ParseCsvPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// Processa arquivos CSV em streaming com validação estrutural e por linha.
@Slf4j
@Component
public class CsvStreamParserAdapter implements ParseCsvPort {

    private static final String DEFAULT_CURRENCY = "BRL";
    private static final char DELIMITER = ';';

    private final IngestionProperties properties;

    public CsvStreamParserAdapter(IngestionProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    // Converte as linhas válidas e registra individualmente as falhas encontradas.
    @Override
    public void parse(ImportJob job, java.io.Reader reader, ImportRecordHandler handler) {
        Objects.requireNonNull(job, "job must not be null");
        Objects.requireNonNull(reader, "reader must not be null");
        Objects.requireNonNull(handler, "handler must not be null");

        IngestionProperties.Upload limits = properties.getUpload();
        BoundedLineReader lines = new BoundedLineReader(reader, limits.getMaxLineLength());

        CsvHeader header = readHeader(lines, limits);
        long dataLines = 0;

        String line;
        while ((line = lines.readLine()) != null) {
            if (!handler.continueProcessing()) {
                log.info("[CsvStreamParserAdapter] - [parse] -> Parse interrompido pelo handler jobId={} linhas={}",
                        job.id(), dataLines);
                return;
            }

            if (line.isBlank()) {
                continue;
            }

            dataLines++;
            if (dataLines > limits.getMaxLines()) {
                throw new FileLineLimitExceededException(limits.getMaxLines());
            }

            long lineNumber = lines.lineNumber();
            try {
                handler.onValid(parseLine(job, line, header, limits));
            } catch (LineParseException e) {
                handler.onInvalid(ImportError.create(
                        job.id(), (int) lineNumber, sample(line), e.errorType, e.getMessage()));
            } catch (RuntimeException e) {
                handler.onInvalid(ImportError.create(
                        job.id(), (int) lineNumber, sample(line), ImportErrorType.PARSE_ERROR,
                        "Unexpected error parsing line"));
            }
        }

        if (dataLines == 0) {
            throw new EmptyFileException("headerOnly=true");
        }

        log.info("[CsvStreamParserAdapter] - [parse] -> CSV parseado jobId={} dataLines={}", job.id(), dataLines);
    }

    // Valida e resolve as colunas declaradas no cabeçalho.
    private CsvHeader readHeader(BoundedLineReader lines, IngestionProperties.Upload limits) {
        String headerLine;
        do {
            headerLine = lines.readLine();
            if (headerLine == null) {
                throw new EmptyFileException("noContent=true");
            }
        } while (headerLine.isBlank());

        List<String> fields;
        try {
            fields = splitCsv(headerLine, limits.getMaxColumns());
        } catch (LineParseException e) {
            throw new InvalidFileHeaderException(List.of(
                    ErrorDetail.ofField("header", "MALFORMED_HEADER", e.getMessage())));
        }

        return CsvHeader.parse(fields, limits.getMaxColumns());
    }

    // Converte uma linha válida em transação bruta.
    private RawTransaction parseLine(ImportJob job,
                                     String line,
                                     CsvHeader header,
                                     IngestionProperties.Upload limits) {

        List<String> fields = splitCsv(line, limits.getMaxColumns());

        if (fields.size() < header.columnCount()) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR,
                    "Expected " + header.columnCount() + " fields but got " + fields.size());
        }

        String dateRaw = required(fields, header.indexOf("date"), "date", limits);
        String amountRaw = required(fields, header.indexOf("amount"), "amount", limits);
        String description = optional(fields, header.indexOf("description"), limits);
        String currencyRaw = optional(fields, header.indexOf("currency"), limits);
        String externalId = optional(fields, header.indexOf("externalId"), limits);

        LocalDate date;
        try {
            date = LocalDate.parse(dateRaw);
        } catch (DateTimeParseException e) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR,
                    "invalid date (expected yyyy-MM-dd)");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountRaw.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR, "invalid amount");
        }

        if (amount.precision() - amount.scale() > 15 || amount.scale() > 4) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR,
                    "amount out of supported range (max 15 integer digits, 4 decimals)");
        }

        String currency = (currencyRaw == null || currencyRaw.isBlank())
                ? DEFAULT_CURRENCY
                : currencyRaw.toUpperCase(Locale.ROOT);

        if (currency.length() != 3) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR,
                    "invalid currency (expected 3-letter code)");
        }

        return RawTransaction.create(
                job.id(),
                externalId,
                description,
                new TransactionDate(date),
                new Money(amount, currency),
                TransactionSourceType.FILE_CSV
        );
    }

    private static String required(List<String> fields, int index, String name,
                                   IngestionProperties.Upload limits) {
        String value = optional(fields, index, limits);
        if (value == null || value.isEmpty()) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR, name + " is required");
        }
        return value;
    }

    private static String optional(List<String> fields, int index, IngestionProperties.Upload limits) {
        if (index < 0 || index >= fields.size()) {
            return null;
        }
        String value = fields.get(index).trim();
        if (value.length() > limits.getMaxFieldLength()) {
            throw new LineParseException(ImportErrorType.VALIDATION_ERROR,
                    "field exceeds max length of " + limits.getMaxFieldLength());
        }
        return value;
    }

    // Divide a linha respeitando campos delimitados por aspas.
    private static List<String> splitCsv(String line, int maxColumns) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
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
                    if (out.size() > maxColumns) {
                        throw new FileColumnLimitExceededException(out.size(), maxColumns);
                    }
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());

        if (out.size() > maxColumns) {
            throw new FileColumnLimitExceededException(out.size(), maxColumns);
        }

        if (inQuotes) {
            throw new LineParseException(ImportErrorType.PARSE_ERROR, "unterminated quoted field");
        }
        return out;
    }

    // Trunca o conteúdo usado no diagnóstico de linhas inválidas.
    private static String sample(String line) {
        int max = 120;
        return line.length() <= max ? line : line.substring(0, max) + "...";
    }

    // Representa uma falha isolada durante a leitura de uma linha.
    private static final class LineParseException extends RuntimeException {

        private final ImportErrorType errorType;

        LineParseException(ImportErrorType errorType, String message) {
            super(message);
            this.errorType = errorType;
        }
    }
}
