package br.com.ecofy.ms_ingestion.adapters.in.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Define o contrato do evento de transação bruta, ignorando campos novos do produtor.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionEventEnvelope(
        String eventId,
        String eventType,
        String eventVersion,
        String userId,
        String sourceSystem,
        List<Transaction> transactions
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(
            String externalId,
            String description,
            LocalDate transactionDate,
            BigDecimal amount,
            String currency
    ) {
    }
}
