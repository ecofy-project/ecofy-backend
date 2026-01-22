package br.com.ecofy.ms_insights.adapters.in.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record BudgetAlertMessage(

        MessageMetadata metadata,

        UUID alertId,

        UUID budgetId,

        UUID userId,

        UUID categoryId,

        String severity,

        String message,

        Integer thresholdPercent,

        long consumedCents,

        long limitCents,

        String currency,

        Instant createdAt

) { }
