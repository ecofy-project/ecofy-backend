package br.com.ecofy.ms_budgeting.core.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record CleanupBudgetsCommand(

        UUID runId,

        LocalDate referenceDate,

        int retentionDays

) {

    // Valida os campos do comando no momento da criação (null checks e retenção >= 0).
    public CleanupBudgetsCommand {
        if (runId == null) throw new IllegalArgumentException("runId must not be null");
        if (referenceDate == null) throw new IllegalArgumentException("referenceDate must not be null");
        if (retentionDays < 0) throw new IllegalArgumentException("retentionDays must be >= 0");
    }

}
