package br.com.ecofy.ms_budgeting.core.application.result;


import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetAlertResult (

        UUID id,

        UUID budgetId,

        UUID consumptionId,

        AlertSeverity severity,

        String message,

        LocalDate periodStart,

        LocalDate periodEnd,

        Instant createdAt

) { }