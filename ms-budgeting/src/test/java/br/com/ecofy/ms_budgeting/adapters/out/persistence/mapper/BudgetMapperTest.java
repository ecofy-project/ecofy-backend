package br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetEntity;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.BudgetKey;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.CategoryId;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Period;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.UserId;
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

class BudgetMapperTest {

    private static final UUID ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID USER_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID CATEGORY_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

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
        Constructor<BudgetMapper> constructor =
                BudgetMapper.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);

        BudgetMapper instance = constructor.newInstance();

        assertNotNull(instance);
    }

    @Test
    void shouldConvertEntityToDomain() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetEntity entity = entity(
                ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.50"),
                "BRL",
                status,
                naturalKey(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END),
                CREATED_AT,
                UPDATED_AT
        );

        Budget domain = BudgetMapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(ID, domain.getId());
        assertEquals(USER_ID, domain.getKey().userId().value());
        assertEquals(CATEGORY_ID, domain.getKey().categoryId().value());
        assertEquals(PERIOD_START, domain.getKey().period().start());
        assertEquals(PERIOD_END, domain.getKey().period().end());
        assertEquals(periodType, domain.getPeriodType());
        assertEquals(new BigDecimal("1000.50"), domain.getLimit().amount());
        assertEquals(BRL, domain.getLimit().currency());
        assertEquals(status, domain.getStatus());
        assertEquals(CREATED_AT, domain.getCreatedAt());
        assertEquals(UPDATED_AT, domain.getUpdatedAt());
    }

    @Test
    void shouldReturnNullWhenEntityIsNullOnToDomain() {
        assertNull(BudgetMapper.toDomain(null));
    }

    @Test
    void shouldTrimCurrencyOnToDomain() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetEntity entity = entity(
                ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("500.25"),
                " BRL ",
                status,
                naturalKey(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END),
                CREATED_AT,
                UPDATED_AT
        );

        Budget domain = BudgetMapper.toDomain(entity);

        assertEquals(BRL, domain.getLimit().currency());
        assertEquals(new BigDecimal("500.25"), domain.getLimit().amount());
    }

    @Test
    void shouldUseCurrentInstantWhenCreatedAtAndUpdatedAtAreNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setCreatedAt(null);
        entity.setUpdatedAt(null);

        Instant before = Instant.now();

        Budget domain = BudgetMapper.toDomain(entity);

        Instant after = Instant.now();

        assertNotNull(domain.getCreatedAt());
        assertNotNull(domain.getUpdatedAt());
        assertEquals(domain.getCreatedAt(), domain.getUpdatedAt());
        assertFalse(domain.getCreatedAt().isBefore(before));
        assertFalse(domain.getCreatedAt().isAfter(after));
    }

    @Test
    void shouldUseCreatedAtAsUpdatedAtWhenUpdatedAtIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(null);

        Budget domain = BudgetMapper.toDomain(entity);

        assertEquals(CREATED_AT, domain.getCreatedAt());
        assertEquals(CREATED_AT, domain.getUpdatedAt());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityIdIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setId(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("id must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityUserIdIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setUserId(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("userId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityCategoryIdIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setCategoryId(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("categoryId must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityPeriodStartIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setPeriodStart(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("periodStart must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEntityPeriodEndIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setPeriodEnd(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("periodEnd must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setCurrency(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("currency must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyIsBlankOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setCurrency("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("currency must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCurrencyIsInvalidOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setCurrency("INVALID");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("Invalid currency code: INVALID", exception.getMessage());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenLimitAmountIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setLimitAmount(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("limitAmount must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenNaturalKeyIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setNaturalKey(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("naturalKey must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenNaturalKeyIsBlankOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setNaturalKey("   ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("naturalKey must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenNaturalKeyDoesNotMatchOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setNaturalKey("different-natural-key");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertTrue(exception.getMessage().contains("naturalKey mismatch. expected="));
        assertTrue(exception.getMessage().contains("persisted=different-natural-key"));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenPeriodTypeIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setPeriodType(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("periodType must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenStatusIsNullOnToDomain() {
        BudgetEntity entity = validEntity();
        entity.setStatus(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toDomain(entity)
        );

        assertEquals("status must not be null", exception.getMessage());
    }

    @Test
    void shouldConvertDomainToEntity() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        Budget domain = domain(
                ID,
                USER_ID,
                CATEGORY_ID,
                PERIOD_START,
                PERIOD_END,
                periodType,
                new BigDecimal("1000.50"),
                BRL,
                status,
                CREATED_AT,
                UPDATED_AT
        );

        BudgetEntity entity = BudgetMapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(ID, entity.getId());
        assertEquals(USER_ID, entity.getUserId());
        assertEquals(CATEGORY_ID, entity.getCategoryId());
        assertEquals(periodType, entity.getPeriodType());
        assertEquals(PERIOD_START, entity.getPeriodStart());
        assertEquals(PERIOD_END, entity.getPeriodEnd());
        assertEquals(new BigDecimal("1000.50"), entity.getLimitAmount());
        assertEquals("BRL", entity.getCurrency());
        assertEquals(status, entity.getStatus());
        assertEquals(naturalKey(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END), entity.getNaturalKey());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
    }

    @Test
    void shouldUseCurrentInstantWhenCreatedAtAndUpdatedAtAreNullOnToEntity() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetKey key = key(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END);
        Money limit = new Money(new BigDecimal("1000.50"), BRL);

        Budget budget = mock(Budget.class);

        when(budget.getId()).thenReturn(ID);
        when(budget.getKey()).thenReturn(key);
        when(budget.getPeriodType()).thenReturn(periodType);
        when(budget.getLimit()).thenReturn(limit);
        when(budget.getStatus()).thenReturn(status);
        when(budget.getCreatedAt()).thenReturn(null);
        when(budget.getUpdatedAt()).thenReturn(null);

        Instant before = Instant.now();

        BudgetEntity entity = BudgetMapper.toEntity(budget);

        Instant after = Instant.now();

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
        assertFalse(entity.getCreatedAt().isBefore(before));
        assertFalse(entity.getCreatedAt().isAfter(after));

        assertEquals(ID, entity.getId());
        assertEquals(USER_ID, entity.getUserId());
        assertEquals(CATEGORY_ID, entity.getCategoryId());
        assertEquals(periodType, entity.getPeriodType());
        assertEquals(PERIOD_START, entity.getPeriodStart());
        assertEquals(PERIOD_END, entity.getPeriodEnd());
        assertEquals(new BigDecimal("1000.50"), entity.getLimitAmount());
        assertEquals("BRL", entity.getCurrency());
        assertEquals(status, entity.getStatus());
        assertEquals(naturalKey(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END), entity.getNaturalKey());
    }

    @Test
    void shouldUseCreatedAtAsUpdatedAtWhenUpdatedAtIsNullOnToEntity() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetKey key = key(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END);
        Money limit = new Money(new BigDecimal("1000.50"), BRL);

        Budget budget = mock(Budget.class);

        when(budget.getId()).thenReturn(ID);
        when(budget.getKey()).thenReturn(key);
        when(budget.getPeriodType()).thenReturn(periodType);
        when(budget.getLimit()).thenReturn(limit);
        when(budget.getStatus()).thenReturn(status);
        when(budget.getCreatedAt()).thenReturn(CREATED_AT);
        when(budget.getUpdatedAt()).thenReturn(null);

        BudgetEntity entity = BudgetMapper.toEntity(budget);

        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(CREATED_AT, entity.getUpdatedAt());

        assertEquals(ID, entity.getId());
        assertEquals(USER_ID, entity.getUserId());
        assertEquals(CATEGORY_ID, entity.getCategoryId());
        assertEquals(periodType, entity.getPeriodType());
        assertEquals(PERIOD_START, entity.getPeriodStart());
        assertEquals(PERIOD_END, entity.getPeriodEnd());
        assertEquals(new BigDecimal("1000.50"), entity.getLimitAmount());
        assertEquals("BRL", entity.getCurrency());
        assertEquals(status, entity.getStatus());
        assertEquals(naturalKey(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END), entity.getNaturalKey());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBudgetIsNullOnToEntity() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toEntity(null)
        );

        assertEquals("budget must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBudgetKeyIsNullOnToEntity() {
        Budget budget = mock(Budget.class);

        when(budget.getKey()).thenReturn(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toEntity(budget)
        );

        assertEquals("budget.key must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBudgetLimitIsNullOnToEntity() {
        Budget budget = mock(Budget.class);

        when(budget.getKey()).thenReturn(key(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END));
        when(budget.getLimit()).thenReturn(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toEntity(budget)
        );

        assertEquals("budget.limit must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBudgetIdIsNullOnToEntity() {
        Budget budget = mock(Budget.class);

        when(budget.getKey()).thenReturn(key(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END));
        when(budget.getLimit()).thenReturn(new Money(new BigDecimal("1000.50"), BRL));
        when(budget.getCreatedAt()).thenReturn(CREATED_AT);
        when(budget.getUpdatedAt()).thenReturn(UPDATED_AT);
        when(budget.getId()).thenReturn(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toEntity(budget)
        );

        assertEquals("id must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenPeriodTypeIsNullOnToEntity() {
        Budget budget = mock(Budget.class);

        when(budget.getKey()).thenReturn(key(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END));
        when(budget.getLimit()).thenReturn(new Money(new BigDecimal("1000.50"), BRL));
        when(budget.getCreatedAt()).thenReturn(CREATED_AT);
        when(budget.getUpdatedAt()).thenReturn(UPDATED_AT);
        when(budget.getId()).thenReturn(ID);
        when(budget.getPeriodType()).thenReturn(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toEntity(budget)
        );

        assertEquals("periodType must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenStatusIsNullOnToEntity() {
        Budget budget = mock(Budget.class);

        when(budget.getKey()).thenReturn(key(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END));
        when(budget.getLimit()).thenReturn(new Money(new BigDecimal("1000.50"), BRL));
        when(budget.getCreatedAt()).thenReturn(CREATED_AT);
        when(budget.getUpdatedAt()).thenReturn(UPDATED_AT);
        when(budget.getId()).thenReturn(ID);
        when(budget.getPeriodType()).thenReturn(anyBudgetPeriodType());
        when(budget.getStatus()).thenReturn(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> BudgetMapper.toEntity(budget)
        );

        assertEquals("status must not be null", exception.getMessage());
    }

    @Test
    void shouldInvokePrivateParseCurrencyWithValidValue() throws Exception {
        Method method = BudgetMapper.class.getDeclaredMethod("parseCurrency", String.class);
        method.setAccessible(true);

        Currency result = (Currency) method.invoke(null, " BRL ");

        assertEquals(BRL, result);
    }

    @Test
    void shouldInvokePrivateParseCurrencyWithNullValue() throws Exception {
        Method method = BudgetMapper.class.getDeclaredMethod("parseCurrency", String.class);
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, new Object[]{null})
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("currency must not be blank", exception.getCause().getMessage());
    }

    @Test
    void shouldInvokePrivateParseCurrencyWithBlankValue() throws Exception {
        Method method = BudgetMapper.class.getDeclaredMethod("parseCurrency", String.class);
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, "   ")
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("currency must not be blank", exception.getCause().getMessage());
    }

    @Test
    void shouldInvokePrivateParseCurrencyWithInvalidValue() throws Exception {
        Method method = BudgetMapper.class.getDeclaredMethod("parseCurrency", String.class);
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, "INVALID")
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertEquals("Invalid currency code: INVALID", exception.getCause().getMessage());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause().getCause());
    }

    @Test
    void shouldInvokePrivateRequireNonNullWithValidValue() throws Exception {
        Method method = BudgetMapper.class.getDeclaredMethod(
                "requireNonNull",
                Object.class,
                String.class
        );

        method.setAccessible(true);

        Object result = method.invoke(null, "value", "field");

        assertEquals("value", result);
    }

    @Test
    void shouldInvokePrivateRequireNonNullWithNullValue() throws Exception {
        Method method = BudgetMapper.class.getDeclaredMethod(
                "requireNonNull",
                Object.class,
                String.class
        );

        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, null, "field")
        );

        assertInstanceOf(NullPointerException.class, exception.getCause());
        assertEquals("field must not be null", exception.getCause().getMessage());
    }

    private static BudgetEntity validEntity() {
        return entity(
                ID,
                USER_ID,
                CATEGORY_ID,
                anyBudgetPeriodType(),
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.50"),
                "BRL",
                anyBudgetStatus(),
                naturalKey(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END),
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static BudgetEntity entity(
            UUID id,
            UUID userId,
            UUID categoryId,
            BudgetPeriodType periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal limitAmount,
            String currency,
            BudgetStatus status,
            String naturalKey,
            Instant createdAt,
            Instant updatedAt
    ) {
        return BudgetEntity.builder()
                .id(id)
                .userId(userId)
                .categoryId(categoryId)
                .periodType(periodType)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .limitAmount(limitAmount)
                .currency(currency)
                .status(status)
                .naturalKey(naturalKey)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private static Budget domain(
            UUID id,
            UUID userId,
            UUID categoryId,
            LocalDate periodStart,
            LocalDate periodEnd,
            BudgetPeriodType periodType,
            BigDecimal amount,
            Currency currency,
            BudgetStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Budget(
                id,
                key(userId, categoryId, periodStart, periodEnd),
                periodType,
                new Money(amount, currency),
                status,
                createdAt,
                updatedAt
        );
    }

    private static BudgetKey key(
            UUID userId,
            UUID categoryId,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        return new BudgetKey(
                new UserId(userId),
                new CategoryId(categoryId),
                new Period(periodStart, periodEnd)
        );
    }

    private static String naturalKey(
            UUID userId,
            UUID categoryId,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        return key(userId, categoryId, periodStart, periodEnd).asNaturalKey();
    }

    private static BudgetPeriodType anyBudgetPeriodType() {
        BudgetPeriodType[] values = BudgetPeriodType.values();

        if (values.length == 0) {
            throw new IllegalStateException("BudgetPeriodType enum must have at least one value");
        }

        return values[0];
    }

    private static BudgetStatus anyBudgetStatus() {
        BudgetStatus[] values = BudgetStatus.values();

        if (values.length == 0) {
            throw new IllegalStateException("BudgetStatus enum must have at least one value");
        }

        return values[0];
    }
}