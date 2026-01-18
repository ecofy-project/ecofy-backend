package br.com.ecofy.ms_budgeting.core.application.result;

import java.time.LocalDate;
import java.util.UUID;

public record CleanupBudgetsResult(

        UUID runId,

        LocalDate referenceDate,

        int retentionDays,

        long budgetsDeleted,

        long consumptionsDeleted

) { }
