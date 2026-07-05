package br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetAlertEntity;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BudgetAlertMapperTest {

    private static final UUID ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID BUDGET_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID CONSUMPTION_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final LocalDate PERIOD_START =
            LocalDate.of(2026, 6, 1);

    private static final LocalDate PERIOD_END =
            LocalDate.of(2026, 6, 30);

    private static final Instant CREATED_AT =
            Instant.parse("2026-06-25T10:30:00Z");

    @Test
    void shouldHavePrivateConstructor() throws Exception {
        Constructor<BudgetAlertMapper> constructor =
                BudgetAlertMapper.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);

        BudgetAlertMapper instance = constructor.newInstance();

        assertNotNull(instance);
    }

    @Test
    void shouldConvertDomainToEntity() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlert domain = domain(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                " Budget reached alert threshold ",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        BudgetAlertEntity entity = BudgetAlertMapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(ID, entity.getId());
        assertEquals(BUDGET_ID, entity.getBudgetId());
        assertEquals(CONSUMPTION_ID, entity.getConsumptionId());
        assertEquals(severity.name(), entity.getSeverity());
        assertEquals("Budget reached alert threshold", entity.getMessage());
        assertEquals(PERIOD_START, entity.getPeriodStart());
        assertEquals(PERIOD_END, entity.getPeriodEnd());
        assertEquals(CREATED_AT, entity.getCreatedAt());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenDomainIsNullOnToEntity() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.toEntity(null)
        );

        assertEquals("domain must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenDomainSeverityIsNullOnToEntity() {
        BudgetAlert domain = domain(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                null,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.toEntity(domain)
        );
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenDomainMessageIsNullOnToEntity() {
        BudgetAlert domain = domain(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                null,
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetAlertMapper.toEntity(domain)
        );

        assertEquals("message must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenDomainMessageIsBlankOnToEntity() {
        BudgetAlert domain = domain(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "   ",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetAlertMapper.toEntity(domain)
        );

        assertEquals("message must not be blank", exception.getMessage());
    }

    @Test
    void shouldConvertEntityToDomain() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity.name(),
                " Budget reached alert threshold ",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        BudgetAlert domain = BudgetAlertMapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(ID, domain.getId());
        assertEquals(BUDGET_ID, domain.getBudgetId());
        assertEquals(CONSUMPTION_ID, domain.getConsumptionId());
        assertEquals(severity, domain.getSeverity());
        assertEquals("Budget reached alert threshold", domain.getMessage());
        assertEquals(PERIOD_START, domain.getPeriodStart());
        assertEquals(PERIOD_END, domain.getPeriodEnd());
        assertEquals(CREATED_AT, domain.getCreatedAt());
    }

    @Test
    void shouldReturnNullWhenEntityIsNullOnToDomain() {
        assertNull(BudgetAlertMapper.toDomain(null));
    }

    @Test
    void shouldUseCurrentInstantWhenEntityCreatedAtIsNullOnToDomain() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity.name(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                null
        );

        Instant before = Instant.now();

        BudgetAlert domain = BudgetAlertMapper.toDomain(entity);

        Instant after = Instant.now();

        assertNotNull(domain.getCreatedAt());
        assertFalse(domain.getCreatedAt().isBefore(before));
        assertFalse(domain.getCreatedAt().isAfter(after));
    }

    @Test
    void shouldParseSeverityWithTrimOnToDomain() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                "  " + severity.name() + "  ",
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        BudgetAlert domain = BudgetAlertMapper.toDomain(entity);

        assertEquals(severity, domain.getSeverity());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityIdIsNullOnToDomain() {
        BudgetAlertEntity entity = entity(
                null,
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity().name(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("id must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityBudgetIdIsNullOnToDomain() {
        BudgetAlertEntity entity = entity(
                ID,
                null,
                CONSUMPTION_ID,
                anyAlertSeverity().name(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("budgetId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityConsumptionIdIsNullOnToDomain() {
        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                null,
                anyAlertSeverity().name(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("consumptionId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityPeriodStartIsNullOnToDomain() {
        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity().name(),
                "Budget reached alert threshold",
                null,
                PERIOD_END,
                CREATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("periodStart must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityPeriodEndIsNullOnToDomain() {
        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity().name(),
                "Budget reached alert threshold",
                PERIOD_START,
                null,
                CREATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("periodEnd must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEntitySeverityIsNullOnToDomain() {
        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                null,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("severity must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEntitySeverityIsBlankOnToDomain() {
        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                "   ",
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("severity must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEntitySeverityIsUnknownOnToDomain() {
        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                "UNKNOWN_SEVERITY",
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("Unknown severity: UNKNOWN_SEVERITY", exception.getMessage());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEntityMessageIsNullOnToDomain() {
        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity().name(),
                null,
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("message must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEntityMessageIsBlankOnToDomain() {
        BudgetAlertEntity entity = entity(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity().name(),
                "\t\n ",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetAlertMapper.toDomain(entity)
        );

        assertEquals("message must not be blank", exception.getMessage());
    }

    @Test
    void shouldCreateNewAlert() {
        AlertSeverity severity = anyAlertSeverity();

        Instant before = Instant.now();

        BudgetAlert alert = BudgetAlertMapper.newAlert(
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                " Budget reached alert threshold ",
                PERIOD_START,
                PERIOD_END
        );

        Instant after = Instant.now();

        assertNotNull(alert);
        assertNotNull(alert.getId());
        assertEquals(BUDGET_ID, alert.getBudgetId());
        assertEquals(CONSUMPTION_ID, alert.getConsumptionId());
        assertEquals(severity, alert.getSeverity());
        assertEquals("Budget reached alert threshold", alert.getMessage());
        assertEquals(PERIOD_START, alert.getPeriodStart());
        assertEquals(PERIOD_END, alert.getPeriodEnd());
        assertNotNull(alert.getCreatedAt());
        assertFalse(alert.getCreatedAt().isBefore(before));
        assertFalse(alert.getCreatedAt().isAfter(after));
    }

    @Test
    void shouldCreateNewAlertsWithDifferentIds() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlert first = BudgetAlertMapper.newAlert(
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        BudgetAlert second = BudgetAlertMapper.newAlert(
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END
        );

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBudgetIdIsNullOnNewAlert() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.newAlert(
                        null,
                        CONSUMPTION_ID,
                        anyAlertSeverity(),
                        "Budget reached alert threshold",
                        PERIOD_START,
                        PERIOD_END
                )
        );

        assertEquals("budgetId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenConsumptionIdIsNullOnNewAlert() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.newAlert(
                        BUDGET_ID,
                        null,
                        anyAlertSeverity(),
                        "Budget reached alert threshold",
                        PERIOD_START,
                        PERIOD_END
                )
        );

        assertEquals("consumptionId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSeverityIsNullOnNewAlert() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.newAlert(
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        null,
                        "Budget reached alert threshold",
                        PERIOD_START,
                        PERIOD_END
                )
        );

        assertEquals("severity must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMessageIsNullOnNewAlert() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetAlertMapper.newAlert(
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        anyAlertSeverity(),
                        null,
                        PERIOD_START,
                        PERIOD_END
                )
        );

        assertEquals("message must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMessageIsBlankOnNewAlert() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetAlertMapper.newAlert(
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        anyAlertSeverity(),
                        "   ",
                        PERIOD_START,
                        PERIOD_END
                )
        );

        assertEquals("message must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenPeriodStartIsNullOnNewAlert() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.newAlert(
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        anyAlertSeverity(),
                        "Budget reached alert threshold",
                        null,
                        PERIOD_END
                )
        );

        assertEquals("periodStart must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenPeriodEndIsNullOnNewAlert() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetAlertMapper.newAlert(
                        BUDGET_ID,
                        CONSUMPTION_ID,
                        anyAlertSeverity(),
                        "Budget reached alert threshold",
                        PERIOD_START,
                        null
                )
        );

        assertEquals("periodEnd must not be null", exception.getMessage());
    }

    @Test
    void shouldInvokePrivateParseSeverityWithValidValue() throws Exception {
        Method method = BudgetAlertMapper.class.getDeclaredMethod("parseSeverity", String.class);
        method.setAccessible(true);

        AlertSeverity severity = anyAlertSeverity();

        Object result = method.invoke(null, "  " + severity.name() + "  ");

        assertEquals(severity, result);
    }

    @Test
    void shouldInvokePrivateNormalizeMessageWithValidValue() throws Exception {
        Method method = BudgetAlertMapper.class.getDeclaredMethod("normalizeMessage", String.class);
        method.setAccessible(true);

        Object result = method.invoke(null, "  message  ");

        assertEquals("message", result);
    }

    @Test
    void shouldInvokePrivateRequireNonNullWithValidValue() throws Exception {
        Method method = BudgetAlertMapper.class.getDeclaredMethod(
                "requireNonNull",
                Object.class,
                String.class
        );

        method.setAccessible(true);

        Object result = method.invoke(null, "value", "field");

        assertEquals("value", result);
    }

    private static BudgetAlert domain(
            UUID id,
            UUID budgetId,
            UUID consumptionId,
            AlertSeverity severity,
            String message,
            LocalDate periodStart,
            LocalDate periodEnd,
            Instant createdAt
    ) {
        BudgetAlert domain = mock(BudgetAlert.class);

        when(domain.getId()).thenReturn(id);
        when(domain.getBudgetId()).thenReturn(budgetId);
        when(domain.getConsumptionId()).thenReturn(consumptionId);
        when(domain.getSeverity()).thenReturn(severity);
        when(domain.getMessage()).thenReturn(message);
        when(domain.getPeriodStart()).thenReturn(periodStart);
        when(domain.getPeriodEnd()).thenReturn(periodEnd);
        when(domain.getCreatedAt()).thenReturn(createdAt);

        return domain;
    }

    private static BudgetAlertEntity entity(
            UUID id,
            UUID budgetId,
            UUID consumptionId,
            String severity,
            String message,
            LocalDate periodStart,
            LocalDate periodEnd,
            Instant createdAt
    ) {
        return BudgetAlertEntity.builder()
                .id(id)
                .budgetId(budgetId)
                .consumptionId(consumptionId)
                .severity(severity)
                .message(message)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .createdAt(createdAt)
                .build();
    }

    private static AlertSeverity anyAlertSeverity() {
        AlertSeverity[] values = AlertSeverity.values();

        if (values.length == 0) {
            throw new IllegalStateException("AlertSeverity enum must have at least one value");
        }

        return values[0];
    }
}