package br.com.ecofy.ms_budgeting.adapters.in.web.dto.response;


import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;

import java.time.Instant;
import java.util.UUID;

public record BudgetAlertResponse(

        UUID id,

        UUID budgetId,

        UUID userId,

        UUID categoryId,

        AlertSeverity severity,

        String message,

        Integer thresholdPercent,

        Long consumedCents,

        Long limitCents,

        String currency,

        Instant createdAt

) { }
