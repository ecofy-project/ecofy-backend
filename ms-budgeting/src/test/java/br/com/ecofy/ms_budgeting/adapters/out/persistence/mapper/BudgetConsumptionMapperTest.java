package br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetConsumptionEntity;
import br.com.ecofy.ms_budgeting.core.domain.BudgetConsumption;
import br.com.ecofy.ms_budgeting.core.domain.enums.ConsumptionSource;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BudgetConsumptionMapperTest {

    private static final UUID ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID BUDGET_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final LocalDate PERIOD_START =
            LocalDate.of(2026, 6, 1);

    private static final LocalDate PERIOD_END =
            LocalDate.of(2026, 6, 30);

    private static final Instant CREATED_AT =
            Instant.parse("2026-06-25T10:30:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-06-25T11:30:00Z");

    private static final Currency BRL = Currency.getInstance("BRL");

    @Test
    void shouldHavePrivateConstructor() throws Exception {
        Constructor<BudgetConsumptionMapper> constructor =
                BudgetConsumptionMapper.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);

        BudgetConsumptionMapper instance = constructor.newInstance();

        assertNotNull(instance);
    }

    @Test
    void shouldConvertDomainToEntity() {
        BudgetConsumption domain = domain(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                new Money(new BigDecimal("123.45"), BRL),
                ConsumptionSource.CATEGORIZED_TX,
                CREATED_AT,
                UPDATED_AT
        );

        BudgetConsumptionEntity entity = BudgetConsumptionMapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(ID, entity.getId());
        assertEquals(BUDGET_ID, entity.getBudgetId());
        assertEquals(PERIOD_START, entity.getPeriodStart());
        assertEquals(PERIOD_END, entity.getPeriodEnd());
        assertEquals(12345L, entity.getConsumedCents());
        assertEquals("BRL", entity.getCurrency());
        assertEquals(ConsumptionSource.CATEGORIZED_TX.name(), entity.getSource());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenDomainIsNullOnToEntity() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.toEntity(null)
        );

        assertEquals("domain must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenConsumedIsNullOnToEntity() {
        BudgetConsumption domain = mock(BudgetConsumption.class);

        when(domain.getConsumed()).thenReturn(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.toEntity(domain)
        );

        assertEquals("domain.consumed must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSourceIsNullOnToEntity() {
        BudgetConsumption domain = domain(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                new Money(new BigDecimal("123.45"), BRL),
                null,
                CREATED_AT,
                UPDATED_AT
        );

        assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.toEntity(domain)
        );
    }

    @Test
    void shouldConvertEntityToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "BRL",
                ConsumptionSource.CATEGORIZED_TX.name(),
                CREATED_AT,
                UPDATED_AT
        );

        BudgetConsumption domain = BudgetConsumptionMapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(ID, domain.getId());
        assertEquals(BUDGET_ID, domain.getBudgetId());
        assertEquals(PERIOD_START, domain.getPeriodStart());
        assertEquals(PERIOD_END, domain.getPeriodEnd());
        assertEquals(new BigDecimal("123.45"), domain.getConsumed().amount());
        assertEquals(BRL, domain.getConsumed().currency());
        assertEquals(ConsumptionSource.CATEGORIZED_TX, domain.getSource());
        assertEquals(CREATED_AT, domain.getCreatedAt());
        assertEquals(UPDATED_AT, domain.getUpdatedAt());
    }

    @Test
    void shouldReturnNullWhenEntityIsNullOnToDomain() {
        assertNull(BudgetConsumptionMapper.toDomain(null));
    }

    @Test
    void shouldTrimCurrencyAndSourceOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                500L,
                " BRL ",
                " " + ConsumptionSource.CATEGORIZED_TX.name() + " ",
                CREATED_AT,
                UPDATED_AT
        );

        BudgetConsumption domain = BudgetConsumptionMapper.toDomain(entity);

        assertEquals(new BigDecimal("5.00"), domain.getConsumed().amount());
        assertEquals(BRL, domain.getConsumed().currency());
        assertEquals(ConsumptionSource.CATEGORIZED_TX, domain.getSource());
    }

    @Test
    void shouldUseCurrentInstantWhenCreatedAtAndUpdatedAtAreNullOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "BRL",
                ConsumptionSource.CATEGORIZED_TX.name(),
                null,
                null
        );

        Instant before = Instant.now();

        BudgetConsumption domain = BudgetConsumptionMapper.toDomain(entity);

        Instant after = Instant.now();

        assertNotNull(domain.getCreatedAt());
        assertNotNull(domain.getUpdatedAt());
        assertEquals(domain.getCreatedAt(), domain.getUpdatedAt());
        assertFalse(domain.getCreatedAt().isBefore(before));
        assertFalse(domain.getCreatedAt().isAfter(after));
    }

    @Test
    void shouldUseCreatedAtAsUpdatedAtWhenEntityUpdatedAtIsNullOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "BRL",
                ConsumptionSource.CATEGORIZED_TX.name(),
                CREATED_AT,
                null
        );

        BudgetConsumption domain = BudgetConsumptionMapper.toDomain(entity);

        assertEquals(CREATED_AT, domain.getCreatedAt());
        assertEquals(CREATED_AT, domain.getUpdatedAt());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityIdIsNullOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                null,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "BRL",
                ConsumptionSource.CATEGORIZED_TX.name(),
                CREATED_AT,
                UPDATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );

        assertEquals("id must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityBudgetIdIsNullOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                null,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "BRL",
                ConsumptionSource.CATEGORIZED_TX.name(),
                CREATED_AT,
                UPDATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );

        assertEquals("budgetId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityPeriodStartIsNullOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                null,
                PERIOD_END,
                12345L,
                "BRL",
                ConsumptionSource.CATEGORIZED_TX.name(),
                CREATED_AT,
                UPDATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );

        assertEquals("periodStart must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityPeriodEndIsNullOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                null,
                12345L,
                "BRL",
                ConsumptionSource.CATEGORIZED_TX.name(),
                CREATED_AT,
                UPDATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );

        assertEquals("periodEnd must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyIsNullOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                null,
                ConsumptionSource.CATEGORIZED_TX.name(),
                CREATED_AT,
                UPDATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );

        assertEquals("currency must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyIsBlankOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "   ",
                ConsumptionSource.CATEGORIZED_TX.name(),
                CREATED_AT,
                UPDATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );

        assertEquals("currency must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyIsInvalidOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "INVALID",
                ConsumptionSource.CATEGORIZED_TX.name(),
                CREATED_AT,
                UPDATED_AT
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenSourceIsNullOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "BRL",
                null,
                CREATED_AT,
                UPDATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );

        assertEquals("source must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenSourceIsBlankOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "BRL",
                "   ",
                CREATED_AT,
                UPDATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );

        assertEquals("source must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenSourceIsInvalidOnToDomain() {
        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "BRL",
                "UNKNOWN_SOURCE",
                CREATED_AT,
                UPDATED_AT
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> BudgetConsumptionMapper.toDomain(entity)
        );
    }

    @Test
    void shouldCreateNewEmptyBudgetConsumption() {
        Instant before = Instant.now();

        BudgetConsumption domain = BudgetConsumptionMapper.newEmpty(
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                " BRL "
        );

        Instant after = Instant.now();

        assertNotNull(domain);
        assertNotNull(domain.getId());
        assertEquals(BUDGET_ID, domain.getBudgetId());
        assertEquals(PERIOD_START, domain.getPeriodStart());
        assertEquals(PERIOD_END, domain.getPeriodEnd());
        assertEquals(new BigDecimal("0.00"), domain.getConsumed().amount());
        assertEquals(BRL, domain.getConsumed().currency());
        assertEquals(ConsumptionSource.CATEGORIZED_TX, domain.getSource());
        assertNotNull(domain.getCreatedAt());
        assertNotNull(domain.getUpdatedAt());
        assertEquals(domain.getCreatedAt(), domain.getUpdatedAt());
        assertFalse(domain.getCreatedAt().isBefore(before));
        assertFalse(domain.getCreatedAt().isAfter(after));
    }

    @Test
    void shouldCreateNewEmptyBudgetConsumptionsWithDifferentIds() {
        BudgetConsumption first = BudgetConsumptionMapper.newEmpty(
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                "BRL"
        );

        BudgetConsumption second = BudgetConsumptionMapper.newEmpty(
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                "BRL"
        );

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBudgetIdIsNullOnNewEmpty() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.newEmpty(
                        null,
                        PERIOD_START,
                        PERIOD_END,
                        "BRL"
                )
        );

        assertEquals("budgetId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenPeriodStartIsNullOnNewEmpty() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.newEmpty(
                        BUDGET_ID,
                        null,
                        PERIOD_END,
                        "BRL"
                )
        );

        assertEquals("periodStart must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenPeriodEndIsNullOnNewEmpty() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetConsumptionMapper.newEmpty(
                        BUDGET_ID,
                        PERIOD_START,
                        null,
                        "BRL"
                )
        );

        assertEquals("periodEnd must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyCodeIsNullOnNewEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetConsumptionMapper.newEmpty(
                        BUDGET_ID,
                        PERIOD_START,
                        PERIOD_END,
                        null
                )
        );

        assertEquals("currencyCode must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyCodeIsBlankOnNewEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetConsumptionMapper.newEmpty(
                        BUDGET_ID,
                        PERIOD_START,
                        PERIOD_END,
                        "   "
                )
        );

        assertEquals("currencyCode must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyCodeIsInvalidOnNewEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BudgetConsumptionMapper.newEmpty(
                        BUDGET_ID,
                        PERIOD_START,
                        PERIOD_END,
                        "INVALID"
                )
        );
    }

    @Test
    void shouldInvokePrivateFromCents() throws Exception {
        Method method = BudgetConsumptionMapper.class.getDeclaredMethod(
                "fromCents",
                long.class,
                Currency.class
        );

        method.setAccessible(true);

        Money result = (Money) method.invoke(null, 12345L, BRL);

        assertEquals(new BigDecimal("123.45"), result.amount());
        assertEquals(BRL, result.currency());
    }

    @Test
    void shouldInvokePrivateToCentsWithHalfUpRounding() throws Exception {
        Method method = BudgetConsumptionMapper.class.getDeclaredMethod(
                "toCents",
                BigDecimal.class
        );

        method.setAccessible(true);

        long result = (long) method.invoke(null, new BigDecimal("123.456"));

        assertEquals(12346L, result);
    }

    @Test
    void shouldInvokePrivateToCentsWithExactScale() throws Exception {
        Method method = BudgetConsumptionMapper.class.getDeclaredMethod(
                "toCents",
                BigDecimal.class
        );

        method.setAccessible(true);

        long result = (long) method.invoke(null, new BigDecimal("123.45"));

        assertEquals(12345L, result);
    }

    @Test
    void shouldInvokePrivateToCentsThrowingWhenAmountIsTooLarge() throws Exception {
        Method method = BudgetConsumptionMapper.class.getDeclaredMethod(
                "toCents",
                BigDecimal.class
        );

        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, new BigDecimal("999999999999999999999999999999999999.99"))
        );

        assertInstanceOf(ArithmeticException.class, exception.getCause());
    }

    @Test
    void shouldInvokePrivateRequireNonNullWithValidValue() throws Exception {
        Method method = BudgetConsumptionMapper.class.getDeclaredMethod(
                "requireNonNull",
                Object.class,
                String.class
        );

        method.setAccessible(true);

        Object result = method.invoke(null, "value", "field");

        assertEquals("value", result);
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithValidValue() throws Exception {
        Method method = BudgetConsumptionMapper.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );

        method.setAccessible(true);

        Object result = method.invoke(null, "  value  ", "field");

        assertEquals("value", result);
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithNullValue() throws Exception {
        Method method = BudgetConsumptionMapper.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );

        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, null, "field")
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("field must not be blank", exception.getCause().getMessage());
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithBlankValue() throws Exception {
        Method method = BudgetConsumptionMapper.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );

        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, "   ", "field")
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("field must not be blank", exception.getCause().getMessage());
    }

    private static BudgetConsumption domain(
            UUID id,
            UUID budgetId,
            LocalDate periodStart,
            LocalDate periodEnd,
            Money consumed,
            ConsumptionSource source,
            Instant createdAt,
            Instant updatedAt
    ) {
        BudgetConsumption domain = mock(BudgetConsumption.class);

        when(domain.getId()).thenReturn(id);
        when(domain.getBudgetId()).thenReturn(budgetId);
        when(domain.getPeriodStart()).thenReturn(periodStart);
        when(domain.getPeriodEnd()).thenReturn(periodEnd);
        when(domain.getConsumed()).thenReturn(consumed);
        when(domain.getSource()).thenReturn(source);
        when(domain.getCreatedAt()).thenReturn(createdAt);
        when(domain.getUpdatedAt()).thenReturn(updatedAt);

        return domain;
    }

    private static BudgetConsumptionEntity entity(
            UUID id,
            UUID budgetId,
            LocalDate periodStart,
            LocalDate periodEnd,
            Long consumedCents,
            String currency,
            String source,
            Instant createdAt,
            Instant updatedAt
    ) {
        return BudgetConsumptionEntity.builder()
                .id(id)
                .budgetId(budgetId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .consumedCents(consumedCents)
                .currency(currency)
                .source(source)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}