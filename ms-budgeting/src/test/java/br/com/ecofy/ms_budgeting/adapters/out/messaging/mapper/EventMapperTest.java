package br.com.ecofy.ms_budgeting.adapters.out.messaging.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.messaging.dto.BudgetAlertEvent;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Verifica que o mapper produz um evento compatível com o contrato do ms-notification.
class EventMapperTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BUDGET_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CONSUMPTION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 6, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 6, 30);
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-25T10:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Test
    void shouldHavePrivateConstructor() throws Exception {
        Constructor<EventMapper> constructor = EventMapper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    void shouldMapEnrichedAlertToNotificationCompatibleEvent() {
        AlertSeverity severity = AlertSeverity.WARNING;
        BudgetAlert alert = enrichedAlert(severity, 80);

        BudgetAlertEvent event = EventMapper.toEvent(alert, FIXED_CLOCK);

        assertEquals(USER_ID, event.userId());
        assertEquals(BUDGET_ID, event.budgetId());
        assertEquals(CATEGORY_ID, event.categoryId());
        assertEquals(new BigDecimal("1000.00"), event.limitAmount());
        assertEquals(new BigDecimal("800.00"), event.consumedAmount());
        assertEquals(80, event.consumedPct());
        assertEquals("WARNING", event.severity());

        assertNotNull(event.metadata());
        assertEquals(expectedEventId(alert), event.metadata().eventId());
        assertEquals(FIXED_INSTANT, event.metadata().occurredAt());
        assertEquals("ms-budgeting", event.metadata().source());
        assertNull(event.metadata().correlationId());
    }

    @Test
    void shouldUseDefaultUtcClockOverload() {
        BudgetAlert alert = enrichedAlert(AlertSeverity.WARNING, 80);

        Instant before = Instant.now();
        BudgetAlertEvent event = EventMapper.toEvent(alert);
        Instant after = Instant.now();

        assertNotNull(event.metadata().occurredAt());
        assertFalse(event.metadata().occurredAt().isBefore(before));
        assertFalse(event.metadata().occurredAt().isAfter(after));
        assertEquals(expectedEventId(alert), event.metadata().eventId());
    }

    @Test
    void shouldGenerateSameDeterministicEventIdForSameAlertData() {
        BudgetAlert first = enrichedAlert(AlertSeverity.WARNING, 80);
        BudgetAlert second = enrichedAlert(AlertSeverity.WARNING, 80);

        assertEquals(
                EventMapper.toEvent(first, FIXED_CLOCK).metadata().eventId(),
                EventMapper.toEvent(second, FIXED_CLOCK).metadata().eventId());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenAlertIsNull() {
        NullPointerException ex = assertThrows(
                NullPointerException.class, () -> EventMapper.toEvent(null, FIXED_CLOCK));
        assertEquals("alert must not be null", ex.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenClockIsNull() {
        BudgetAlert alert = enrichedAlert(AlertSeverity.WARNING, 80);
        NullPointerException ex = assertThrows(
                NullPointerException.class, () -> EventMapper.toEvent(alert, null));
        assertEquals("clock must not be null", ex.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenBudgetIdIsNull() {
        BudgetAlert alert = mock(BudgetAlert.class);
        when(alert.getBudgetId()).thenReturn(null);
        lenient().when(alert.getSeverity()).thenReturn(AlertSeverity.WARNING);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> EventMapper.toEvent(alert, FIXED_CLOCK));
        assertEquals("alert.budgetId must not be null", ex.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenSeverityIsNull() {
        BudgetAlert alert = mock(BudgetAlert.class);
        when(alert.getBudgetId()).thenReturn(BUDGET_ID);
        when(alert.getSeverity()).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> EventMapper.toEvent(alert, FIXED_CLOCK));
        assertEquals("alert.severity must not be null", ex.getMessage());
    }

    private static BudgetAlert enrichedAlert(AlertSeverity severity, int pct) {
        BudgetAlert alert = mock(BudgetAlert.class);
        when(alert.getBudgetId()).thenReturn(BUDGET_ID);
        when(alert.getSeverity()).thenReturn(severity);
        when(alert.getUserId()).thenReturn(USER_ID);
        when(alert.getCategoryId()).thenReturn(CATEGORY_ID);
        when(alert.getLimitAmount()).thenReturn(new BigDecimal("1000.00"));
        when(alert.getConsumedAmount()).thenReturn(new BigDecimal("800.00"));
        when(alert.getConsumedPct()).thenReturn(pct);
        lenient().when(alert.getConsumptionId()).thenReturn(CONSUMPTION_ID);
        lenient().when(alert.getPeriodStart()).thenReturn(PERIOD_START);
        lenient().when(alert.getPeriodEnd()).thenReturn(PERIOD_END);
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
}
