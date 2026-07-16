package br.com.ecofy.ms_budgeting.adapters.out.messaging.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Correção Dia 6 (item #10): o evento agora espelha o contrato consumido pelo ms-notification
 * (userId, budgetId, categoryId, limitAmount, consumedAmount, consumedPct, severity, metadata).
 */
class BudgetAlertEventTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BUDGET_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final BigDecimal LIMIT = new BigDecimal("1000.00");
    private static final BigDecimal CONSUMED = new BigDecimal("800.00");
    private static final Integer PCT = 80;
    private static final String SEVERITY = "WARNING";
    private static final Instant OCCURRED_AT = Instant.parse("2026-06-25T10:30:00Z");

    private static BudgetAlertEvent.EventMetadata metadata() {
        return new BudgetAlertEvent.EventMetadata("event-001", "corr-001", OCCURRED_AT, "ms-budgeting");
    }

    private static BudgetAlertEvent baseEvent() {
        return new BudgetAlertEvent(USER_ID, BUDGET_ID, CATEGORY_ID, LIMIT, CONSUMED, PCT, SEVERITY, metadata());
    }

    @Test
    void shouldCreateBudgetAlertEventWithAllFields() {
        BudgetAlertEvent event = baseEvent();

        assertEquals(USER_ID, event.userId());
        assertEquals(BUDGET_ID, event.budgetId());
        assertEquals(CATEGORY_ID, event.categoryId());
        assertEquals(LIMIT, event.limitAmount());
        assertEquals(CONSUMED, event.consumedAmount());
        assertEquals(PCT, event.consumedPct());
        assertEquals(SEVERITY, event.severity());
        assertNotNull(event.metadata());
        assertEquals("event-001", event.metadata().eventId());
        assertEquals("corr-001", event.metadata().correlationId());
        assertEquals(OCCURRED_AT, event.metadata().occurredAt());
        assertEquals("ms-budgeting", event.metadata().source());
    }

    @Test
    void shouldCreateBudgetAlertEventWithNullFields() {
        BudgetAlertEvent event = new BudgetAlertEvent(null, null, null, null, null, null, null, null);

        assertNull(event.userId());
        assertNull(event.budgetId());
        assertNull(event.categoryId());
        assertNull(event.limitAmount());
        assertNull(event.consumedAmount());
        assertNull(event.consumedPct());
        assertNull(event.severity());
        assertNull(event.metadata());
    }

    @Test
    void shouldCompareByAllRecordComponents() {
        BudgetAlertEvent event = baseEvent();
        BudgetAlertEvent same = baseEvent();

        BudgetAlertEvent different = new BudgetAlertEvent(
                USER_ID, BUDGET_ID, CATEGORY_ID, LIMIT, CONSUMED, 90, SEVERITY, metadata());

        assertEquals(event, same);
        assertEquals(event.hashCode(), same.hashCode());
        assertNotEquals(event, different);
        assertNotEquals(event, null);
        assertNotEquals(event, "not-a-budget-alert-event");
    }

    @Test
    void shouldNotBeEqualWhenUserIdChanges() {
        BudgetAlertEvent event = baseEvent();
        BudgetAlertEvent different = new BudgetAlertEvent(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                BUDGET_ID, CATEGORY_ID, LIMIT, CONSUMED, PCT, SEVERITY, metadata());

        assertNotEquals(event, different);
    }

    @Test
    void shouldExposeMetadataFieldsAndToString() {
        BudgetAlertEvent event = baseEvent();
        String result = event.toString();

        assertTrue(result.contains("BudgetAlertEvent"));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("budgetId=" + BUDGET_ID));
        assertTrue(result.contains("severity=" + SEVERITY));
    }
}
