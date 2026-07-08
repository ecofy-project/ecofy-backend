package br.com.ecofy.ms_budgeting.core.application.result;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BudgetResultTest {

    @Test
    void shouldCreateBudgetResultWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BudgetPeriodType periodType = BudgetPeriodType.values()[0];
        LocalDate periodStart = LocalDate.of(2026, 1, 1);
        LocalDate periodEnd = LocalDate.of(2026, 1, 31);
        BigDecimal limitAmount = new BigDecimal("1500.00");
        String currency = "BRL";
        BudgetStatus status = BudgetStatus.values()[0];
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        BudgetResult result = new BudgetResult(
                id,
                userId,
                categoryId,
                periodType,
                periodStart,
                periodEnd,
                limitAmount,
                currency,
                status,
                createdAt,
                updatedAt
        );

        assertEquals(id, result.id());
        assertEquals(userId, result.userId());
        assertEquals(categoryId, result.categoryId());
        assertEquals(periodType, result.periodType());
        assertEquals(periodStart, result.periodStart());
        assertEquals(periodEnd, result.periodEnd());
        assertEquals(limitAmount, result.limitAmount());
        assertEquals(currency, result.currency());
        assertEquals(status, result.status());
        assertEquals(createdAt, result.createdAt());
        assertEquals(updatedAt, result.updatedAt());
    }

    @Test
    void shouldValidateEqualsHashCodeAndToString() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BudgetPeriodType periodType = BudgetPeriodType.values()[0];
        LocalDate periodStart = LocalDate.of(2026, 1, 1);
        LocalDate periodEnd = LocalDate.of(2026, 1, 31);
        BigDecimal limitAmount = new BigDecimal("1500.00");
        String currency = "BRL";
        BudgetStatus status = BudgetStatus.values()[0];
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        BudgetResult result = new BudgetResult(
                id,
                userId,
                categoryId,
                periodType,
                periodStart,
                periodEnd,
                limitAmount,
                currency,
                status,
                createdAt,
                updatedAt
        );

        BudgetResult sameResult = new BudgetResult(
                id,
                userId,
                categoryId,
                periodType,
                periodStart,
                periodEnd,
                limitAmount,
                currency,
                status,
                createdAt,
                updatedAt
        );

        BudgetResult differentResult = new BudgetResult(
                UUID.randomUUID(),
                userId,
                categoryId,
                periodType,
                periodStart,
                periodEnd,
                limitAmount,
                currency,
                status,
                createdAt,
                updatedAt
        );

        assertEquals(result, sameResult);
        assertEquals(result.hashCode(), sameResult.hashCode());
        assertNotEquals(result, differentResult);
        assertNotNull(result.toString());
    }
}