package br.com.ecofy.ms_insights.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.Map;

// Representa o evento de insight criado no formato consumido pelo ms-notification.
public record InsightCreatedEvent(

        String eventId,

        String type,

        String userId,

        String insightId,

        String insightType,

        int score,

        String periodStart,

        String periodEnd,

        Instant createdAt,

        Map<String, Object> payload,

        EventMetadata metadata

) {

    // Transporta os metadados de rastreamento no formato esperado pelo consumidor.
    public record EventMetadata(
            String eventId,
            String correlationId,
            Instant occurredAt,
            String source
    ) { }
}
