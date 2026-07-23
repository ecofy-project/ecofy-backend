package br.com.ecofy.ms_categorization.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.UUID;

// Define o envelope uniforme de evento, separando metadados de transporte do payload de negócio.
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String correlationId,
        UUID causationId,
        T data
) {
}
