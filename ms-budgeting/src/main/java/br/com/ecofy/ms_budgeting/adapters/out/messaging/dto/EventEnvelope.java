package br.com.ecofy.ms_budgeting.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.UUID;

// Envelopa eventos no formato uniforme do EcoFy, separando metadados de rastreamento do conteúdo de negócio.
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
