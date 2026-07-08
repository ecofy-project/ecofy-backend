package br.com.ecofy.ms_budgeting.core.application.result;

import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BudgetOverviewResultTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BUDGET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CONSUMPTION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ALERT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final AlertSeverity SEVERITY = AlertSeverity.values()[0];
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 7, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 7, 31);
    private static final Instant CREATED_AT = Instant.parse("2026-07-05T10:00:00Z");

    @Test
    void shouldCreateBudgetOverviewResult() {
        List<BudgetConsumptionResult> consumptions = consumptions();
        List<BudgetAlertResult> alerts = alerts();
        BudgetOverviewResult result = new BudgetOverviewResult(USER_ID, consumptions, alerts);
        assertNotNull(result);
        assertEquals(USER_ID, result.userId());
        assertEquals(consumptions, result.consumptions());
        assertEquals(alerts, result.alerts());
    }

    @Test
    void shouldAllowAllFieldsNullBecauseRecordHasNoValidation() {
        BudgetOverviewResult result = new BudgetOverviewResult(null, null, null);
        assertNotNull(result);
        assertNull(result.userId());
        assertNull(result.consumptions());
        assertNull(result.alerts());
    }

    @Test
    void shouldAllowEmptyLists() {
        BudgetOverviewResult result = new BudgetOverviewResult(USER_ID, List.of(), List.of());
        assertNotNull(result);
        assertEquals(USER_ID, result.userId());
        assertTrue(result.consumptions().isEmpty());
        assertTrue(result.alerts().isEmpty());
    }

    @Test
    void shouldAllowListsWithNullElementsBecauseRecordHasNoValidation() {
        List<BudgetConsumptionResult> consumptions = Arrays.asList(consumption(), null);
        List<BudgetAlertResult> alerts = Arrays.asList(alert(), null);
        BudgetOverviewResult result = new BudgetOverviewResult(USER_ID, consumptions, alerts);
        assertNotNull(result);
        assertEquals(consumptions, result.consumptions());
        assertEquals(alerts, result.alerts());
        assertNull(result.consumptions().get(1));
        assertNull(result.alerts().get(1));
    }

    @Test
    void shouldKeepSameListReferencesBecauseRecordDoesNotCreateDefensiveCopies() {
        List<BudgetConsumptionResult> consumptions = new ArrayList<>(List.of(consumption()));
        List<BudgetAlertResult> alerts = new ArrayList<>(List.of(alert()));
        BudgetOverviewResult result = new BudgetOverviewResult(USER_ID, consumptions, alerts);
        assertSame(consumptions, result.consumptions());
        assertSame(alerts, result.alerts());
    }

    @Test
    void shouldReflectExternalListMutationBecauseRecordDoesNotCreateDefensiveCopies() {
        List<BudgetConsumptionResult> consumptions = new ArrayList<>();
        List<BudgetAlertResult> alerts = new ArrayList<>();
        BudgetOverviewResult result = new BudgetOverviewResult(USER_ID, consumptions, alerts);
        consumptions.add(consumption());
        alerts.add(alert());
        assertEquals(1, result.consumptions().size());
        assertEquals(1, result.alerts().size());
        assertEquals(consumption(), result.consumptions().get(0));
        assertEquals(alert(), result.alerts().get(0));
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreEqual() {
        BudgetOverviewResult result = result();
        BudgetOverviewResult sameResult = result();
        assertEquals(result, sameResult);
        assertEquals(result.hashCode(), sameResult.hashCode());
    }

    @Test
    void shouldReturnTrueWhenComparingSameInstance() {
        BudgetOverviewResult result = result();
        assertEquals(result, result);
    }

    @Test
    void shouldNotBeEqualWhenUserIdIsDifferent() {
        BudgetOverviewResult different = new BudgetOverviewResult(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), consumptions(), alerts());
        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenConsumptionsAreDifferent() {
        BudgetOverviewResult different = new BudgetOverviewResult(USER_ID, List.of(new BudgetConsumptionResult(BUDGET_ID, new BigDecimal("999.99"), new BigDecimal("2500.00"), new BigDecimal("39.99"))), alerts());
        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenAlertsAreDifferent() {
        BudgetOverviewResult different = new BudgetOverviewResult(USER_ID, consumptions(), List.of(new BudgetAlertResult(ALERT_ID, BUDGET_ID, CONSUMPTION_ID, SEVERITY, "Different alert message", PERIOD_START, PERIOD_END, CREATED_AT)));
        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenOneUserIdIsNull() {
        BudgetOverviewResult different = new BudgetOverviewResult(null, consumptions(), alerts());
        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenOneConsumptionsListIsNull() {
        BudgetOverviewResult different = new BudgetOverviewResult(USER_ID, null, alerts());
        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenOneAlertsListIsNull() {
        BudgetOverviewResult different = new BudgetOverviewResult(USER_ID, consumptions(), null);
        assertNotEquals(result(), different);
    }

    @Test
    void shouldNotBeEqualWhenListOrderIsDifferent() {
        BudgetConsumptionResult firstConsumption = consumption();
        BudgetConsumptionResult secondConsumption = new BudgetConsumptionResult(BUDGET_ID, new BigDecimal("1000.00"), new BigDecimal("3000.00"), new BigDecimal("33.33"));
        BudgetOverviewResult result = new BudgetOverviewResult(USER_ID, List.of(firstConsumption, secondConsumption), alerts());
        BudgetOverviewResult different = new BudgetOverviewResult(USER_ID, List.of(secondConsumption, firstConsumption), alerts());
        assertNotEquals(result, different);
    }

    @Test
    void shouldNotBeEqualToNull() {
        assertNotEquals(null, result());
    }

    @Test
    void shouldNotBeEqualToDifferentType() {
        assertNotEquals("not-a-budget-overview-result", result());
    }

    @Test
    void shouldBeEqualWhenAllComponentsAreNull() {
        BudgetOverviewResult result = new BudgetOverviewResult(null, null, null);
        BudgetOverviewResult sameResult = new BudgetOverviewResult(null, null, null);
        assertEquals(result, sameResult);
        assertEquals(result.hashCode(), sameResult.hashCode());
    }

    @Test
    void shouldGenerateToStringWithAllComponents() {
        BudgetOverviewResult result = result();
        String stringResult = result.toString();
        assertNotNull(stringResult);
        assertTrue(stringResult.contains("BudgetOverviewResult"));
        assertTrue(stringResult.contains("userId=" + USER_ID));
        assertTrue(stringResult.contains("consumptions="));
        assertTrue(stringResult.contains("alerts="));
        assertTrue(stringResult.contains("BudgetConsumptionResult"));
        assertTrue(stringResult.contains("BudgetAlertResult"));
    }

    @Test
    void shouldGenerateToStringWithNullComponents() {
        BudgetOverviewResult result = new BudgetOverviewResult(null, null, null);
        String stringResult = result.toString();
        assertNotNull(stringResult);
        assertTrue(stringResult.contains("BudgetOverviewResult"));
        assertTrue(stringResult.contains("userId=null"));
        assertTrue(stringResult.contains("consumptions=null"));
        assertTrue(stringResult.contains("alerts=null"));
    }

    @Test
    void shouldBeRecord() {
        assertTrue(BudgetOverviewResult.class.isRecord());
    }

    @Test
    void shouldHaveExpectedRecordComponents() {
        RecordComponent[] components = BudgetOverviewResult.class.getRecordComponents();
        assertNotNull(components);
        assertEquals(3, components.length);
        assertArrayEquals(new String[]{"userId", "consumptions", "alerts"}, Arrays.stream(components).map(RecordComponent::getName).toArray(String[]::new));
        assertArrayEquals(new Class<?>[]{UUID.class, List.class, List.class}, Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new));
    }

    private static BudgetOverviewResult result() {
        return new BudgetOverviewResult(USER_ID, consumptions(), alerts());
    }

    private static List<BudgetConsumptionResult> consumptions() {
        return List.of(consumption());
    }

    private static List<BudgetAlertResult> alerts() {
        return List.of(alert());
    }

    private static BudgetConsumptionResult consumption() {
        return new BudgetConsumptionResult(BUDGET_ID, new BigDecimal("750.50"), new BigDecimal("2500.00"), new BigDecimal("30.02"));
    }

    private static BudgetAlertResult alert() {
        return new BudgetAlertResult(ALERT_ID, BUDGET_ID, CONSUMPTION_ID, SEVERITY, "Budget alert generated", PERIOD_START, PERIOD_END, CREATED_AT);
    }
}