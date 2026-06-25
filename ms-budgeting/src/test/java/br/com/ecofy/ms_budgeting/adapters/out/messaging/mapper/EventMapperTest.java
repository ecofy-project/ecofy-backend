package br.com.ecofy.ms_budgeting.adapters.out.messaging.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.messaging.dto.BudgetAlertEvent;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventMapperTest {

    private static final UUID BUDGET_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID CONSUMPTION_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final LocalDate PERIOD_START =
            LocalDate.of(2026, 6, 1);

    private static final LocalDate PERIOD_END =
            LocalDate.of(2026, 6, 30);

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-06-25T10:30:00Z");

    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Test
    void shouldHavePrivateConstructor() throws Exception {
        Constructor<EventMapper> constructor = EventMapper.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);

        EventMapper instance = constructor.newInstance();

        assertNotNull(instance);
    }

    @Test
    void shouldMapBudgetAlertToBudgetAlertEventUsingProvidedClock() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                " Budget reached alert threshold ",
                PERIOD_START,
                PERIOD_END
        );

        BudgetAlertEvent event = EventMapper.toEvent(alert, FIXED_CLOCK);

        assertEquals(expectedEventId(alert), event.eventId());
        assertEquals(FIXED_INSTANT, event.occurredAt());
        assertEquals(BUDGET_ID, event.budgetId());
        assertEquals(CONSUMPTION_ID, event.consumptionId());
        assertEquals(severity, event.severity());
        assertEquals("Budget reached alert threshold", event.message());
        assertEquals(PERIOD_START, event.periodStart());
        assertEquals(PERIOD_END, event.periodEnd());
    }

    @Test
    void shouldMapBudgetAlertToBudgetAlertEventUsingDefaultUtcClock() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        Instant before = Instant.now();

        BudgetAlertEvent event = EventMapper.toEvent(alert);

        Instant after = Instant.now();

        assertEquals(expectedEventId(alert), event.eventId());
        assertNotNull(event.occurredAt());
        assertFalse(event.occurredAt().isBefore(before));
        assertFalse(event.occurredAt().isAfter(after));
        assertEquals(BUDGET_ID, event.budgetId());
        assertEquals(CONSUMPTION_ID, event.consumptionId());
        assertEquals(severity, event.severity());
        assertEquals("Budget reached alert threshold", event.message());
        assertEquals(PERIOD_START, event.periodStart());
        assertEquals(PERIOD_END, event.periodEnd());
    }

    @Test
    void shouldGenerateSameDeterministicEventIdForSameAlertData() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlert firstAlert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "First message",
                PERIOD_START,
                PERIOD_END
        );

        BudgetAlert secondAlert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Second message",
                PERIOD_START,
                PERIOD_END
        );

        BudgetAlertEvent firstEvent = EventMapper.toEvent(firstAlert, FIXED_CLOCK);
        BudgetAlertEvent secondEvent = EventMapper.toEvent(secondAlert, FIXED_CLOCK);

        assertEquals(firstEvent.eventId(), secondEvent.eventId());
    }

    @Test
    void shouldGenerateDifferentDeterministicEventIdWhenBudgetIdChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlert firstAlert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        BudgetAlert secondAlert = alert(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        BudgetAlertEvent firstEvent = EventMapper.toEvent(firstAlert, FIXED_CLOCK);
        BudgetAlertEvent secondEvent = EventMapper.toEvent(secondAlert, FIXED_CLOCK);

        assertNotEquals(firstEvent.eventId(), secondEvent.eventId());
    }

    @Test
    void shouldAcceptNullOptionalFieldsUsedInDeterministicEventId() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlert alert = alert(
                BUDGET_ID,
                null,
                severity,
                "Budget reached alert threshold",
                null,
                null
        );

        BudgetAlertEvent event = EventMapper.toEvent(alert, FIXED_CLOCK);

        assertEquals(expectedEventId(alert), event.eventId());
        assertEquals(FIXED_INSTANT, event.occurredAt());
        assertEquals(BUDGET_ID, event.budgetId());
        assertNull(event.consumptionId());
        assertEquals(severity, event.severity());
        assertEquals("Budget reached alert threshold", event.message());
        assertNull(event.periodStart());
        assertNull(event.periodEnd());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenAlertIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> EventMapper.toEvent(null, FIXED_CLOCK)
        );

        assertEquals("alert must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenClockIsNull() {
        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> EventMapper.toEvent(alert, null)
        );

        assertEquals("clock must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenBudgetIdIsNull() {
        BudgetAlert alert = alert(
                null,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EventMapper.toEvent(alert, FIXED_CLOCK)
        );

        assertEquals("alert.budgetId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenSeverityIsNull() {
        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                null,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EventMapper.toEvent(alert, FIXED_CLOCK)
        );

        assertEquals("alert.severity must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMessageIsNull() {
        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                null,
                PERIOD_START,
                PERIOD_END
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EventMapper.toEvent(alert, FIXED_CLOCK)
        );

        assertEquals("alert.message must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMessageIsBlank() {
        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "   ",
                PERIOD_START,
                PERIOD_END
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EventMapper.toEvent(alert, FIXED_CLOCK)
        );

        assertEquals("alert.message must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMessageIsEmptyAfterTrim() {
        BudgetAlert alert = alert(
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "\t\n ",
                PERIOD_START,
                PERIOD_END
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EventMapper.toEvent(alert, FIXED_CLOCK)
        );

        assertEquals("alert.message must not be blank", exception.getMessage());
    }

    private static BudgetAlert alert(
            UUID budgetId,
            UUID consumptionId,
            AlertSeverity severity,
            String message,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        BudgetAlert alert = mock(BudgetAlert.class);

        when(alert.getBudgetId()).thenReturn(budgetId);
        when(alert.getConsumptionId()).thenReturn(consumptionId);
        when(alert.getSeverity()).thenReturn(severity);
        when(alert.getMessage()).thenReturn(message);
        when(alert.getPeriodStart()).thenReturn(periodStart);
        when(alert.getPeriodEnd()).thenReturn(periodEnd);

        return alert;
    }

    private static String expectedEventId(BudgetAlert alert) {
        String base = String.valueOf(alert.getBudgetId())
                + "|" + String.valueOf(alert.getConsumptionId())
                + "|" + String.valueOf(alert.getSeverity())
                + "|" + String.valueOf(alert.getPeriodStart())
                + "|" + String.valueOf(alert.getPeriodEnd());

        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static AlertSeverity anyAlertSeverity() {
        AlertSeverity[] values = AlertSeverity.values();

        if (values.length == 0) {
            throw new IllegalStateException("AlertSeverity enum must have at least one value");
        }

        return values[0];
    }
}