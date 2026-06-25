package br.com.ecofy.ms_budgeting.adapters.out.messaging.dto;

import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BudgetAlertEventTest {

    private static final String EVENT_ID = "event-001";

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-06-25T10:30:00Z");

    private static final UUID BUDGET_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID CONSUMPTION_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final LocalDate PERIOD_START =
            LocalDate.of(2026, 6, 1);

    private static final LocalDate PERIOD_END =
            LocalDate.of(2026, 6, 30);

    @Test
    void shouldCreateBudgetAlertEventWithAllFields() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = new BudgetAlertEvent(
                EVENT_ID,
                OCCURRED_AT,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        assertEquals(EVENT_ID, event.eventId());
        assertEquals(OCCURRED_AT, event.occurredAt());
        assertEquals(BUDGET_ID, event.budgetId());
        assertEquals(CONSUMPTION_ID, event.consumptionId());
        assertEquals(severity, event.severity());
        assertEquals("Budget reached alert threshold", event.message());
        assertEquals(PERIOD_START, event.periodStart());
        assertEquals(PERIOD_END, event.periodEnd());
    }

    @Test
    void shouldCreateBudgetAlertEventWithNullFields() {
        BudgetAlertEvent event = new BudgetAlertEvent(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertNull(event.eventId());
        assertNull(event.occurredAt());
        assertNull(event.budgetId());
        assertNull(event.consumptionId());
        assertNull(event.severity());
        assertNull(event.message());
        assertNull(event.periodStart());
        assertNull(event.periodEnd());
    }

    @Test
    void shouldCompareBudgetAlertEventByAllRecordComponents() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = baseEvent(severity);
        BudgetAlertEvent sameEvent = baseEvent(severity);

        BudgetAlertEvent differentEvent = new BudgetAlertEvent(
                "event-002",
                OCCURRED_AT,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        assertEquals(event, event);
        assertEquals(event, sameEvent);
        assertNotEquals(event, differentEvent);
        assertNotEquals(event, null);
        assertNotEquals(event, "not-a-budget-alert-event");
    }

    @Test
    void shouldGenerateHashCodeUsingAllRecordComponents() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = baseEvent(severity);
        BudgetAlertEvent sameEvent = baseEvent(severity);

        assertEquals(event, sameEvent);
        assertEquals(event.hashCode(), sameEvent.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenOccurredAtChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = baseEvent(severity);

        BudgetAlertEvent differentEvent = new BudgetAlertEvent(
                EVENT_ID,
                OCCURRED_AT.plusSeconds(60),
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        assertNotEquals(event, differentEvent);
    }

    @Test
    void shouldNotBeEqualWhenBudgetIdChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = baseEvent(severity);

        BudgetAlertEvent differentEvent = new BudgetAlertEvent(
                EVENT_ID,
                OCCURRED_AT,
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        assertNotEquals(event, differentEvent);
    }

    @Test
    void shouldNotBeEqualWhenConsumptionIdChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = baseEvent(severity);

        BudgetAlertEvent differentEvent = new BudgetAlertEvent(
                EVENT_ID,
                OCCURRED_AT,
                BUDGET_ID,
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        assertNotEquals(event, differentEvent);
    }

    @Test
    void shouldNotBeEqualWhenSeverityChanges() {
        AlertSeverity[] severities = AlertSeverity.values();

        if (severities.length < 2) {
            return;
        }

        BudgetAlertEvent event = baseEvent(severities[0]);

        BudgetAlertEvent differentEvent = new BudgetAlertEvent(
                EVENT_ID,
                OCCURRED_AT,
                BUDGET_ID,
                CONSUMPTION_ID,
                severities[1],
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        assertNotEquals(event, differentEvent);
    }

    @Test
    void shouldNotBeEqualWhenMessageChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = baseEvent(severity);

        BudgetAlertEvent differentEvent = new BudgetAlertEvent(
                EVENT_ID,
                OCCURRED_AT,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Different alert message",
                PERIOD_START,
                PERIOD_END
        );

        assertNotEquals(event, differentEvent);
    }

    @Test
    void shouldNotBeEqualWhenPeriodStartChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = baseEvent(severity);

        BudgetAlertEvent differentEvent = new BudgetAlertEvent(
                EVENT_ID,
                OCCURRED_AT,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START.plusDays(1),
                PERIOD_END
        );

        assertNotEquals(event, differentEvent);
    }

    @Test
    void shouldNotBeEqualWhenPeriodEndChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = baseEvent(severity);

        BudgetAlertEvent differentEvent = new BudgetAlertEvent(
                EVENT_ID,
                OCCURRED_AT,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END.plusDays(1)
        );

        assertNotEquals(event, differentEvent);
    }

    @Test
    void shouldReturnToStringWithRecordComponents() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEvent event = baseEvent(severity);

        String result = event.toString();

        assertTrue(result.contains("BudgetAlertEvent"));
        assertTrue(result.contains("eventId=" + EVENT_ID));
        assertTrue(result.contains("occurredAt=" + OCCURRED_AT));
        assertTrue(result.contains("budgetId=" + BUDGET_ID));
        assertTrue(result.contains("consumptionId=" + CONSUMPTION_ID));
        assertTrue(result.contains("severity=" + severity));
        assertTrue(result.contains("message=Budget reached alert threshold"));
        assertTrue(result.contains("periodStart=" + PERIOD_START));
        assertTrue(result.contains("periodEnd=" + PERIOD_END));
    }

    private static BudgetAlertEvent baseEvent(AlertSeverity severity) {
        return new BudgetAlertEvent(
                EVENT_ID,
                OCCURRED_AT,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );
    }

    private static AlertSeverity anyAlertSeverity() {
        AlertSeverity[] values = AlertSeverity.values();

        if (values.length == 0) {
            throw new IllegalStateException("AlertSeverity enum must have at least one value");
        }

        return values[0];
    }
}