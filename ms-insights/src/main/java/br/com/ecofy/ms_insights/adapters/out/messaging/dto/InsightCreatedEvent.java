package br.com.ecofy.ms_insights.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Evento {@code insight.created} publicado em {@code eco.insight.created} e consumido pelo ms-notification.
 *
 * <p>Correção Dia 8 (item #8/#11): o contrato foi ENRIQUECIDO para casar com o consumidor
 * {@code InsightCreatedEventMessage} do ms-notification, que lê {@code userId, insightId, insightType,
 * periodStart, periodEnd, metadata{eventId,...}}. Antes o evento expunha apenas {@code type} (com o valor
 * "insight.created") e não trazia {@code insightType}/período/metadata, então a notificação perdia o tipo
 * do insight e a idempotência por eventId. Campos antigos foram mantidos (backward-compatible); o
 * <b>nome do tópico não mudou</b>.</p>
 */
public record InsightCreatedEvent(

        String eventId,

        String type,            // tipo do evento ("insight.created") — mantido p/ compatibilidade

        String userId,

        String insightId,

        String insightType,     // tipo de domínio do insight (ex.: SPENDING_BREAKDOWN) — usado pelo ms-notification

        int score,

        String periodStart,     // usado pelo ms-notification

        String periodEnd,       // usado pelo ms-notification

        Instant createdAt,

        Map<String, Object> payload,

        EventMetadata metadata

) {

    /** Metadados espelhando {@code MessageMetadata} do ms-notification (eventId/correlationId/occurredAt/source). */
    public record EventMetadata(
            String eventId,
            String correlationId,
            Instant occurredAt,
            String source
    ) { }
}
