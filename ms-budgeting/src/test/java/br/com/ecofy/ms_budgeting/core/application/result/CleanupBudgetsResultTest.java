package br.com.ecofy.ms_budgeting.core.application.result;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CleanupBudgetsResultTest {

    @Test
    void shouldCreateCleanupBudgetsResultWithAllFields() {
        UUID runId = UUID.randomUUID();
        LocalDate referenceDate = LocalDate.of(2026, 1, 31);
        int retentionDays = 90;
        long budgetsDeleted = 10L;
        long consumptionsDeleted = 25L;

        CleanupBudgetsResult result = new CleanupBudgetsResult(
                runId,
                referenceDate,
                retentionDays,
                budgetsDeleted,
                consumptionsDeleted
        );

        assertEquals(runId, result.runId());
        assertEquals(referenceDate, result.referenceDate());
        assertEquals(retentionDays, result.retentionDays());
        assertEquals(budgetsDeleted, result.budgetsDeleted());
        assertEquals(consumptionsDeleted, result.consumptionsDeleted());
    }

    @Test
    void shouldValidateEqualsHashCodeAndToString() {
        UUID runId = UUID.randomUUID();
        LocalDate referenceDate = LocalDate.of(2026, 1, 31);
        int retentionDays = 90;
        long budgetsDeleted = 10L;
        long consumptionsDeleted = 25L;

        CleanupBudgetsResult result = new CleanupBudgetsResult(
                runId,
                referenceDate,
                retentionDays,
                budgetsDeleted,
                consumptionsDeleted
        );

        CleanupBudgetsResult sameResult = new CleanupBudgetsResult(
                runId,
                referenceDate,
                retentionDays,
                budgetsDeleted,
                consumptionsDeleted
        );

        CleanupBudgetsResult differentResult = new CleanupBudgetsResult(
                UUID.randomUUID(),
                referenceDate,
                retentionDays,
                budgetsDeleted,
                consumptionsDeleted
        );

        assertEquals(result, sameResult);
        assertEquals(result.hashCode(), sameResult.hashCode());
        assertNotEquals(result, differentResult);
        assertNotNull(result.toString());
    }
}