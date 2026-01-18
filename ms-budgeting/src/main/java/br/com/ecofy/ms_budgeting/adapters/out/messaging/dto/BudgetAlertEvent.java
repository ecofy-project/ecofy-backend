package br.com.ecofy.ms_budgeting.adapters.out.messaging.dto;


import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetAlertEvent (
        
        String eventId,

        Instant occurredAt,

        UUID budgetId,

        UUID consumptionId,

        AlertSeverity severity,

        String message,

        LocalDate periodStart,

        LocalDate periodEnd

) { }
