package br.com.ecofy.ms_budgeting.core.application.result;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BudgetConsumptionResultTest {

    private static final UUID BUDGET_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final BigDecimal CONSUMED_AMOUNT =
            new BigDecimal("750.50");

    private static final BigDecimal LIMIT_AMOUNT =
            new BigDecimal("2500.00");

    private static final BigDecimal CONSUMED_PCT =
            new BigDecimal("30.02");

    @Test
    void shouldCreateBudgetConsumptionResult() {
        BudgetConsumptionResult result = result();

        assertNotNull(result);
        assertEquals(BUDGET_ID, result.budgetId());
        assertEquals(CONSUMED_AMOUNT, result.consumedAmount());
        assertEquals(LIMIT_AMOUNT, result.limitAmount());
        assertEquals(CONSUMED_PCT, result.consumedPct());
    }

    @Test
    void shouldAllowAllFieldsNullBecauseRecordHasNoValidation() {
        BudgetConsumptionResult result =
                new BudgetConsumptionResult(
                        null,
                        null,
                        null,
                        null
                );

        assertNotNull(result);
        assertNull(result.budgetId());
        assertNull(result.consumedAmount());
        assertNull(result.limitAmount());
        assertNull(result.consumedPct());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreEqual() {
        BudgetConsumptionResult result = result();
        BudgetConsumptionResult sameResult = result();

        assertEquals(result, sameResult);
        assertEquals(result.hashCode(), sameResult.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameInstance() {
        BudgetConsumptionResult result = result();

        assertEquals(result, result);
    }

    @Test
    void shouldNotBeEqualWhenBudgetIdIsDifferent() {
        BudgetConsumptionResult different =
                new BudgetConsumptionResult(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        CONSUMED_AMOUNT,
                        LIMIT_AMOUNT,
                        CONSUMED_PCT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenConsumedAmountIsDifferent() {
        BudgetConsumptionResult different =
                new BudgetConsumptionResult(
                        BUDGET_ID,
                        new BigDecimal("999.99"),
                        LIMIT_AMOUNT,
                        CONSUMED_PCT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenLimitAmountIsDifferent() {
        BudgetConsumptionResult different =
                new BudgetConsumptionResult(
                        BUDGET_ID,
                        CONSUMED_AMOUNT,
                        new BigDecimal("5000.00"),
                        CONSUMED_PCT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenConsumedPctIsDifferent() {
        BudgetConsumptionResult different =
                new BudgetConsumptionResult(
                        BUDGET_ID,
                        CONSUMED_AMOUNT,
                        LIMIT_AMOUNT,
                        new BigDecimal("99.99")
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenOneBudgetIdIsNull() {
        BudgetConsumptionResult different =
                new BudgetConsumptionResult(
                        null,
                        CONSUMED_AMOUNT,
                        LIMIT_AMOUNT,
                        CONSUMED_PCT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenOneConsumedAmountIsNull() {
        BudgetConsumptionResult different =
                new BudgetConsumptionResult(
                        BUDGET_ID,
                        null,
                        LIMIT_AMOUNT,
                        CONSUMED_PCT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenOneLimitAmountIsNull() {
        BudgetConsumptionResult different =
                new BudgetConsumptionResult(
                        BUDGET_ID,
                        CONSUMED_AMOUNT,
                        null,
                        CONSUMED_PCT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenOneConsumedPctIsNull() {
        BudgetConsumptionResult different =
                new BudgetConsumptionResult(
                        BUDGET_ID,
                        CONSUMED_AMOUNT,
                        LIMIT_AMOUNT,
                        null
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenBigDecimalScaleIsDifferent() {
        BudgetConsumptionResult result =
                new BudgetConsumptionResult(
                        BUDGET_ID,
                        new BigDecimal("750.50"),
                        LIMIT_AMOUNT,
                        CONSUMED_PCT
                );

        BudgetConsumptionResult different =
                new BudgetConsumptionResult(
                        BUDGET_ID,
                        new BigDecimal("750.500"),
                        LIMIT_AMOUNT,
                        CONSUMED_PCT
                );

        assertNotEquals(result, different);
    }

    @Test
    void shouldNotBeEqualToNull() {
        assertNotEquals(null, result());
    }

    @Test
    void shouldNotBeEqualToDifferentType() {
        assertNotEquals("not-a-budget-consumption-result", result());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreNull() {
        BudgetConsumptionResult result =
                new BudgetConsumptionResult(
                        null,
                        null,
                        null,
                        null
                );

        BudgetConsumptionResult sameResult =
                new BudgetConsumptionResult(
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(result, sameResult);
        assertEquals(result.hashCode(), sameResult.hashCode());
    }

    @Test
    void shouldGenerateToStringWithAllComponents() {
        BudgetConsumptionResult result = result();

        String stringResult = result.toString();

        assertNotNull(stringResult);
        assertTrue(stringResult.contains("BudgetConsumptionResult"));
        assertTrue(stringResult.contains("budgetId=" + BUDGET_ID));
        assertTrue(stringResult.contains("consumedAmount=" + CONSUMED_AMOUNT));
        assertTrue(stringResult.contains("limitAmount=" + LIMIT_AMOUNT));
        assertTrue(stringResult.contains("consumedPct=" + CONSUMED_PCT));
    }

    @Test
    void shouldGenerateToStringWithNullComponents() {
        BudgetConsumptionResult result =
                new BudgetConsumptionResult(
                        null,
                        null,
                        null,
                        null
                );

        String stringResult = result.toString();

        assertNotNull(stringResult);
        assertTrue(stringResult.contains("BudgetConsumptionResult"));
        assertTrue(stringResult.contains("budgetId=null"));
        assertTrue(stringResult.contains("consumedAmount=null"));
        assertTrue(stringResult.contains("limitAmount=null"));
        assertTrue(stringResult.contains("consumedPct=null"));
    }

    @Test
    void shouldBeRecord() {
        assertTrue(BudgetConsumptionResult.class.isRecord());
    }

    @Test
    void shouldHaveExpectedRecordComponents() {
        RecordComponent[] components =
                BudgetConsumptionResult.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(4, components.length);

        assertArrayEquals(
                new String[]{
                        "budgetId",
                        "consumedAmount",
                        "limitAmount",
                        "consumedPct"
                },
                Arrays.stream(components)
                        .map(RecordComponent::getName)
                        .toArray(String[]::new)
        );

        assertArrayEquals(
                new Class<?>[]{
                        UUID.class,
                        BigDecimal.class,
                        BigDecimal.class,
                        BigDecimal.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }

    private static BudgetConsumptionResult result() {
        return new BudgetConsumptionResult(
                BUDGET_ID,
                CONSUMED_AMOUNT,
                LIMIT_AMOUNT,
                CONSUMED_PCT
        );
    }
}