package br.com.ecofy.ms_budgeting.core.application.result;

import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BudgetAlertResultTest {

    private static final UUID ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID BUDGET_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID CONSUMPTION_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final AlertSeverity SEVERITY =
            AlertSeverity.values()[0];

    private static final String MESSAGE =
            "Budget alert generated";

    private static final LocalDate PERIOD_START =
            LocalDate.of(2026, 7, 1);

    private static final LocalDate PERIOD_END =
            LocalDate.of(2026, 7, 31);

    private static final Instant CREATED_AT =
            Instant.parse("2026-07-05T10:00:00Z");

    @Test
    void shouldCreateBudgetAlertResult() {
        BudgetAlertResult result = result();

        assertNotNull(result);
        assertEquals(ID, result.id());
        assertEquals(BUDGET_ID, result.budgetId());
        assertEquals(CONSUMPTION_ID, result.consumptionId());
        assertEquals(SEVERITY, result.severity());
        assertEquals(MESSAGE, result.message());
        assertEquals(PERIOD_START, result.periodStart());
        assertEquals(PERIOD_END, result.periodEnd());
        assertEquals(CREATED_AT, result.createdAt());
    }

    @Test
    void shouldAllowAllFieldsNullBecauseRecordHasNoValidation() {
        BudgetAlertResult result =
                new BudgetAlertResult(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertNotNull(result);
        assertNull(result.id());
        assertNull(result.budgetId());
        assertNull(result.consumptionId());
        assertNull(result.severity());
        assertNull(result.message());
        assertNull(result.periodStart());
        assertNull(result.periodEnd());
        assertNull(result.createdAt());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreEqual() {
        BudgetAlertResult result = result();
        BudgetAlertResult sameResult = result();

        assertEquals(result, sameResult);
        assertEquals(result.hashCode(), sameResult.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameInstance() {
        BudgetAlertResult result = result();

        assertEquals(result, result);
    }

    @Test
    void shouldNotBeEqualWhenIdIsDifferent() {
        BudgetAlertResult different =
                new BudgetAlertResult(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        SEVERITY,
                        MESSAGE,
                        PERIOD_START,
                        PERIOD_END,
                        CREATED_AT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenBudgetIdIsDifferent() {
        BudgetAlertResult different =
                new BudgetAlertResult(
                        ID,
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        CONSUMPTION_ID,
                        SEVERITY,
                        MESSAGE,
                        PERIOD_START,
                        PERIOD_END,
                        CREATED_AT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenConsumptionIdIsDifferent() {
        BudgetAlertResult different =
                new BudgetAlertResult(
                        ID,
                        BUDGET_ID,
                        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                        SEVERITY,
                        MESSAGE,
                        PERIOD_START,
                        PERIOD_END,
                        CREATED_AT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenSeverityIsDifferent() {
        BudgetAlertResult different =
                new BudgetAlertResult(
                        ID,
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        null,
                        MESSAGE,
                        PERIOD_START,
                        PERIOD_END,
                        CREATED_AT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenMessageIsDifferent() {
        BudgetAlertResult different =
                new BudgetAlertResult(
                        ID,
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        SEVERITY,
                        "Different alert message",
                        PERIOD_START,
                        PERIOD_END,
                        CREATED_AT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenPeriodStartIsDifferent() {
        BudgetAlertResult different =
                new BudgetAlertResult(
                        ID,
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        SEVERITY,
                        MESSAGE,
                        PERIOD_START.plusDays(1),
                        PERIOD_END,
                        CREATED_AT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenPeriodEndIsDifferent() {
        BudgetAlertResult different =
                new BudgetAlertResult(
                        ID,
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        SEVERITY,
                        MESSAGE,
                        PERIOD_START,
                        PERIOD_END.plusDays(1),
                        CREATED_AT
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenCreatedAtIsDifferent() {
        BudgetAlertResult different =
                new BudgetAlertResult(
                        ID,
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        SEVERITY,
                        MESSAGE,
                        PERIOD_START,
                        PERIOD_END,
                        CREATED_AT.plusSeconds(1)
                );

        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualToNull() {
        assertNotEquals(null, result());
    }

    @Test
    void shouldNotBeEqualToDifferentType() {
        assertNotEquals("not-a-budget-alert-result", result());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreNull() {
        BudgetAlertResult result =
                new BudgetAlertResult(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        BudgetAlertResult sameResult =
                new BudgetAlertResult(
                        null,
                        null,
                        null,
                        null,
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
        BudgetAlertResult result = result();

        String stringResult = result.toString();

        assertNotNull(stringResult);
        assertTrue(stringResult.contains("BudgetAlertResult"));
        assertTrue(stringResult.contains("id=" + ID));
        assertTrue(stringResult.contains("budgetId=" + BUDGET_ID));
        assertTrue(stringResult.contains("consumptionId=" + CONSUMPTION_ID));
        assertTrue(stringResult.contains("severity=" + SEVERITY));
        assertTrue(stringResult.contains("message=" + MESSAGE));
        assertTrue(stringResult.contains("periodStart=" + PERIOD_START));
        assertTrue(stringResult.contains("periodEnd=" + PERIOD_END));
        assertTrue(stringResult.contains("createdAt=" + CREATED_AT));
    }

    @Test
    void shouldGenerateToStringWithNullComponents() {
        BudgetAlertResult result =
                new BudgetAlertResult(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        String stringResult = result.toString();

        assertNotNull(stringResult);
        assertTrue(stringResult.contains("BudgetAlertResult"));
        assertTrue(stringResult.contains("id=null"));
        assertTrue(stringResult.contains("budgetId=null"));
        assertTrue(stringResult.contains("consumptionId=null"));
        assertTrue(stringResult.contains("severity=null"));
        assertTrue(stringResult.contains("message=null"));
        assertTrue(stringResult.contains("periodStart=null"));
        assertTrue(stringResult.contains("periodEnd=null"));
        assertTrue(stringResult.contains("createdAt=null"));
    }

    @Test
    void shouldBeRecord() {
        assertTrue(BudgetAlertResult.class.isRecord());
    }

    @Test
    void shouldHaveExpectedRecordComponents() {
        RecordComponent[] components =
                BudgetAlertResult.class.getRecordComponents();

        assertNotNull(components);
        assertEquals(8, components.length);

        assertArrayEquals(
                new String[]{
                        "id",
                        "budgetId",
                        "consumptionId",
                        "severity",
                        "message",
                        "periodStart",
                        "periodEnd",
                        "createdAt"
                },
                Arrays.stream(components)
                        .map(RecordComponent::getName)
                        .toArray(String[]::new)
        );

        assertArrayEquals(
                new Class<?>[]{
                        UUID.class,
                        UUID.class,
                        UUID.class,
                        AlertSeverity.class,
                        String.class,
                        LocalDate.class,
                        LocalDate.class,
                        Instant.class
                },
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
        );
    }

    private static BudgetAlertResult result() {
        return new BudgetAlertResult(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                SEVERITY,
                MESSAGE,
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );
    }
}