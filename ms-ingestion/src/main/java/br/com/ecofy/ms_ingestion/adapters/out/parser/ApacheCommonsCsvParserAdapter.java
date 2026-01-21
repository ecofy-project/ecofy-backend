package br.com.ecofy.ms_ingestion.adapters.out.parser;

import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import br.com.ecofy.ms_ingestion.core.port.out.ParseCsvPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ApacheCommonsCsvParserAdapter implements ParseCsvPort {

    private static final String DEFAULT_CURRENCY = "BRL";

    // Converte o conteúdo CSV de um job em uma lista de RawTransaction, validando linhas e aplicando defaults.
    @Override
    public List<RawTransaction> parse(ImportJob job, String csvContent) {
        log.info("[ApacheCommonsCsvParserAdapter] - [parse] -> Parsing CSV para jobId={}", job.id());

        List<RawTransaction> result = new ArrayList<>();

        // Formato esperado (simples): date;description;amount;currency
        try (StringReader reader = new StringReader(csvContent)) {
            String[] lines = csvContent.split("\\R");
            int lineNo = 0;

            for (String line : lines) {
                lineNo++;

                // pula header
                if (lineNo == 1 && line.toLowerCase().contains("date")) {
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length < 3) {
                    log.warn("[ApacheCommonsCsvParserAdapter] - [parse] -> Linha inválida line={} content={}",
                            lineNo, line);
                    continue;
                }

                LocalDate date = LocalDate.parse(parts[0].trim());
                String desc = parts[1].trim();
                BigDecimal amount = new BigDecimal(parts[2].trim());
                String currencyCode = parts.length >= 4
                        ? parts[3].trim()
                        : DEFAULT_CURRENCY;

                RawTransaction tx = RawTransaction.create(
                        job.id(),
                        null,
                        desc,
                        new TransactionDate(date),
                        new Money(amount, currencyCode),
                        TransactionSourceType.FILE_CSV
                );

                result.add(tx);
            }
        } catch (Exception e) {
            log.error("[ApacheCommonsCsvParserAdapter] - [parse] -> Erro ao parsear CSV jobId={} error={}",
                    job.id(), e.getMessage(), e);
            throw new IllegalArgumentException("Error parsing CSV", e);
        }

        log.info("[ApacheCommonsCsvParserAdapter] - [parse] -> CSV parseado com sucesso totalTx={}", result.size());
        return result;
    }

}
