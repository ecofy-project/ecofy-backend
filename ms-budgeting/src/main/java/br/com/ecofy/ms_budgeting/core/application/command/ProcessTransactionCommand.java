package br.com.ecofy.ms_budgeting.core.application.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProcessTransactionCommand(

        UUID runId,

        UUID transactionId,

        UUID userId,

        UUID categoryId,

        BigDecimal amount,

        String currency,

        LocalDate transactionDate,

        EventMetadata metadata

) {

    // Valida campos obrigatórios, regras básicas (amount >= 0) e normaliza a moeda.
    public ProcessTransactionCommand {
        require(runId, "runId");
        require(transactionId, "transactionId");
        require(userId, "userId");
        require(amount, "amount");
        require(transactionDate, "transactionDate");
        require(metadata, "metadata");

        if (amount.signum() < 0) throw new IllegalArgumentException("amount must be >= 0");

        currency = normalizeCurrency(currency);
    }

    // Normaliza a moeda para uppercase e restringe para moedas suportadas.
    private static String normalizeCurrency(String raw) {
        if (raw == null) throw new IllegalArgumentException("currency must not be null");
        String c = raw.trim().toUpperCase();

        return switch (c) {
            case "BRL", "USD", "EUR" -> c;
            default -> throw new IllegalArgumentException("Unsupported currency: " + c);
        };
    }

    public record EventMetadata(
            String eventId,
            String correlationId,
            String topic,
            int partition,
            long offset,
            String key,
            Instant receivedAt
    ) {
        // Valida metadados mínimos do evento Kafka (topic e receivedAt).
        public EventMetadata {
            if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic is required");
            if (receivedAt == null) throw new IllegalArgumentException("receivedAt must not be null");
        }
    }

    // Centraliza validação de null para campos obrigatórios.
    private static void require(Object v, String name) {
        if (v == null) throw new IllegalArgumentException(name + " must not be null");
    }

}
