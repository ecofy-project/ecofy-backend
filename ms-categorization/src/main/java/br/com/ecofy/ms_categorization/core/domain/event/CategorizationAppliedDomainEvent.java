package br.com.ecofy.ms_categorization.core.domain.event;

import java.time.Instant;
import java.util.UUID;

// Representa o evento de domínio de categorização aplicada, independente de DTOs de transporte.
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
