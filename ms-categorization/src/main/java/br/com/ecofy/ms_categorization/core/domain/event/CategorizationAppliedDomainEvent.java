package br.com.ecofy.ms_categorization.core.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de DOMÍNIO (core) de categorização aplicada (auditoria). Sem dependência de
 * DTOs Kafka/Jackson/Spring/adapters — o adapter converte para o DTO Kafka de saída.
 */
public record CategorizationAppliedDomainEvent(
        UUID eventId,
        UUID transactionId,
        UUID categoryId,
        UUID ruleId,
        String mode,
        int score,
        UUID suggestionId,
        Instant occurredAt
) { }
