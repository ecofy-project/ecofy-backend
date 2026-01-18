package br.com.ecofy.ms_categorization.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record CategorizationAppliedEvent(

        UUID eventId,

        UUID transactionId,

        UUID categoryId,

        UUID ruleId,

        String mode,

        int score,

        UUID suggestionId,

        Instant occurredAt

) { }