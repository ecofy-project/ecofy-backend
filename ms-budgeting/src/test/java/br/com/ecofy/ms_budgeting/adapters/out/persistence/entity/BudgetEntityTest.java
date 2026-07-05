package br.com.ecofy.ms_budgeting.adapters.out.persistence.entity;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BudgetEntityTest {

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

    private static final LocalDate ARCHIVED_AT =
            LocalDate.of(2026, 7, 10);

    private static final Instant CREATED_AT =
            Instant.parse("2026-06-25T10:30:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-06-25T11:30:00Z");

    @Test
    void shouldCreateBudgetEntityUsingNoArgsConstructorAndSetters() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetEntity entity = new BudgetEntity();

        entity.setId(ID);
        entity.setUserId(USER_ID);
        entity.setCategoryId(CATEGORY_ID);
        entity.setPeriodType(periodType);
        entity.setPeriodStart(PERIOD_START);
        entity.setPeriodEnd(PERIOD_END);
        entity.setLimitAmount(new BigDecimal("1000.50"));
        entity.setCurrency("BRL");
        entity.setStatus(status);
        entity.setArchivedAt(ARCHIVED_AT);
        entity.setNaturalKey("user-category-month");
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);

        assertEquals(ID, entity.getId());
        assertEquals(USER_ID, entity.getUserId());
        assertEquals(CATEGORY_ID, entity.getCategoryId());
        assertEquals(periodType, entity.getPeriodType());
        assertEquals(PERIOD_START, entity.getPeriodStart());
        assertEquals(PERIOD_END, entity.getPeriodEnd());
        assertEquals(new BigDecimal("1000.50"), entity.getLimitAmount());
        assertEquals("BRL", entity.getCurrency());
        assertEquals(status, entity.getStatus());
        assertEquals(ARCHIVED_AT, entity.getArchivedAt());
        assertEquals("user-category-month", entity.getNaturalKey());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
    }

    @Test
    void shouldCreateBudgetEntityUsingAllArgsConstructor() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetEntity entity = new BudgetEntity(
                ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.50"),
                "BRL",
                status,
                ARCHIVED_AT,
                "user-category-month",
                CREATED_AT,
                UPDATED_AT
        );

        assertEquals(ID, entity.getId());
        assertEquals(USER_ID, entity.getUserId());
        assertEquals(CATEGORY_ID, entity.getCategoryId());
        assertEquals(periodType, entity.getPeriodType());
        assertEquals(PERIOD_START, entity.getPeriodStart());
        assertEquals(PERIOD_END, entity.getPeriodEnd());
        assertEquals(new BigDecimal("1000.50"), entity.getLimitAmount());
        assertEquals("BRL", entity.getCurrency());
        assertEquals(status, entity.getStatus());
        assertEquals(ARCHIVED_AT, entity.getArchivedAt());
        assertEquals("user-category-month", entity.getNaturalKey());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
    }

    @Test
    void shouldCreateBudgetEntityUsingBuilder() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetEntity entity = BudgetEntity.builder()
                .id(ID)
                .userId(USER_ID)
                .categoryId(CATEGORY_ID)
                .periodType(periodType)
                .periodStart(PERIOD_START)
                .periodEnd(PERIOD_END)
                .limitAmount(new BigDecimal("1000.50"))
                .currency("BRL")
                .status(status)
                .archivedAt(ARCHIVED_AT)
                .naturalKey("user-category-month")
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();

        assertEquals(ID, entity.getId());
        assertEquals(USER_ID, entity.getUserId());
        assertEquals(CATEGORY_ID, entity.getCategoryId());
        assertEquals(periodType, entity.getPeriodType());
        assertEquals(PERIOD_START, entity.getPeriodStart());
        assertEquals(PERIOD_END, entity.getPeriodEnd());
        assertEquals(new BigDecimal("1000.50"), entity.getLimitAmount());
        assertEquals("BRL", entity.getCurrency());
        assertEquals(status, entity.getStatus());
        assertEquals(ARCHIVED_AT, entity.getArchivedAt());
        assertEquals("user-category-month", entity.getNaturalKey());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
    }

    @Test
    void shouldAllowNullFieldsThroughLombokGeneratedAccessors() {
        BudgetEntity entity = new BudgetEntity();

        entity.setId(null);
        entity.setUserId(null);
        entity.setCategoryId(null);
        entity.setPeriodType(null);
        entity.setPeriodStart(null);
        entity.setPeriodEnd(null);
        entity.setLimitAmount(null);
        entity.setCurrency(null);
        entity.setStatus(null);
        entity.setArchivedAt(null);
        entity.setNaturalKey(null);
        entity.setCreatedAt(null);
        entity.setUpdatedAt(null);

        assertNull(entity.getId());
        assertNull(entity.getUserId());
        assertNull(entity.getCategoryId());
        assertNull(entity.getPeriodType());
        assertNull(entity.getPeriodStart());
        assertNull(entity.getPeriodEnd());
        assertNull(entity.getLimitAmount());
        assertNull(entity.getCurrency());
        assertNull(entity.getStatus());
        assertNull(entity.getArchivedAt());
        assertNull(entity.getNaturalKey());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void shouldInitializeIdCreatedAtUpdatedAtAndNormalizeFieldsOnPrePersist() {
        BudgetEntity entity = BudgetEntity.builder()
                .id(null)
                .userId(USER_ID)
                .categoryId(CATEGORY_ID)
                .periodType(anyBudgetPeriodType())
                .periodStart(PERIOD_START)
                .periodEnd(PERIOD_END)
                .limitAmount(new BigDecimal("1000.50"))
                .currency(" brl ")
                .status(anyBudgetStatus())
                .naturalKey("  natural-key-001  ")
                .createdAt(null)
                .updatedAt(null)
                .build();

        Instant before = Instant.now();

        entity.prePersist();

        Instant after = Instant.now();

        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());

        assertFalse(entity.getCreatedAt().isBefore(before));
        assertFalse(entity.getCreatedAt().isAfter(after));

        assertFalse(entity.getUpdatedAt().isBefore(before));
        assertFalse(entity.getUpdatedAt().isAfter(after));

        assertEquals("BRL", entity.getCurrency());
        assertEquals("natural-key-001", entity.getNaturalKey());
    }

    @Test
    void shouldPreserveExistingIdAndCreatedAtOnPrePersist() {
        BudgetEntity entity = BudgetEntity.builder()
                .id(ID)
                .userId(USER_ID)
                .categoryId(CATEGORY_ID)
                .periodType(anyBudgetPeriodType())
                .periodStart(PERIOD_START)
                .periodEnd(PERIOD_END)
                .limitAmount(new BigDecimal("1000.50"))
                .currency(" usd ")
                .status(anyBudgetStatus())
                .naturalKey("  natural-key-002  ")
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();

        entity.prePersist();

        assertEquals(ID, entity.getId());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertNotEquals(UPDATED_AT, entity.getUpdatedAt());
        assertEquals("USD", entity.getCurrency());
        assertEquals("natural-key-002", entity.getNaturalKey());
    }

    @Test
    void shouldHandleNullCurrencyAndNullNaturalKeyOnPrePersist() {
        BudgetEntity entity = BudgetEntity.builder()
                .id(null)
                .currency(null)
                .naturalKey(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

        entity.prePersist();

        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertNull(entity.getCurrency());
        assertNull(entity.getNaturalKey());
    }

    @Test
    void shouldUpdateUpdatedAtAndNormalizeCurrencyOnPreUpdate() {
        BudgetEntity entity = BudgetEntity.builder()
                .id(ID)
                .currency(" eur ")
                .updatedAt(UPDATED_AT)
                .build();

        Instant before = Instant.now();

        entity.preUpdate();

        Instant after = Instant.now();

        assertNotNull(entity.getUpdatedAt());
        assertFalse(entity.getUpdatedAt().isBefore(before));
        assertFalse(entity.getUpdatedAt().isAfter(after));
        assertEquals("EUR", entity.getCurrency());
    }

    @Test
    void shouldHandleNullCurrencyOnPreUpdate() {
        BudgetEntity entity = BudgetEntity.builder()
                .id(ID)
                .currency(null)
                .updatedAt(UPDATED_AT)
                .build();

        entity.preUpdate();

        assertNotNull(entity.getUpdatedAt());
        assertNull(entity.getCurrency());
    }

    @Test
    void shouldHaveEntityAnnotation() {
        assertNotNull(BudgetEntity.class.getAnnotation(Entity.class));
    }

    @Test
    void shouldHaveTableAnnotationWithUniqueNaturalKeyConstraint() {
        Table table = BudgetEntity.class.getAnnotation(Table.class);

        assertNotNull(table);
        assertEquals("budgets", table.name());
        assertEquals(1, table.uniqueConstraints().length);

        UniqueConstraint uniqueConstraint = table.uniqueConstraints()[0];

        assertEquals("uk_budget_natural", uniqueConstraint.name());
        assertArrayEquals(new String[]{"natural_key"}, uniqueConstraint.columnNames());
    }

    @Test
    void shouldHaveIdColumnMapping() throws Exception {
        assertNotNull(fieldAnnotation("id", Id.class));

        Column column = fieldAnnotation("id", Column.class);

        assertEquals("id", column.name());
        assertFalse(column.nullable());
        assertFalse(column.updatable());
    }

    @Test
    void shouldHaveUserIdColumnMapping() throws Exception {
        Column column = fieldAnnotation("userId", Column.class);

        assertEquals("user_id", column.name());
        assertFalse(column.nullable());
        assertFalse(column.updatable());
    }

    @Test
    void shouldHaveCategoryIdColumnMapping() throws Exception {
        Column column = fieldAnnotation("categoryId", Column.class);

        assertEquals("category_id", column.name());
        assertFalse(column.nullable());
        assertFalse(column.updatable());
    }

    @Test
    void shouldHavePeriodTypeColumnMapping() throws Exception {
        Enumerated enumerated = fieldAnnotation("periodType", Enumerated.class);
        Column column = fieldAnnotation("periodType", Column.class);

        assertEquals(EnumType.STRING, enumerated.value());
        assertEquals("period_type", column.name());
        assertFalse(column.nullable());
    }

    @Test
    void shouldHavePeriodStartColumnMapping() throws Exception {
        Column column = fieldAnnotation("periodStart", Column.class);

        assertEquals("period_start", column.name());
        assertFalse(column.nullable());
    }

    @Test
    void shouldHavePeriodEndColumnMapping() throws Exception {
        Column column = fieldAnnotation("periodEnd", Column.class);

        assertEquals("period_end", column.name());
        assertFalse(column.nullable());
    }

    @Test
    void shouldHaveLimitAmountColumnMapping() throws Exception {
        Column column = fieldAnnotation("limitAmount", Column.class);

        assertEquals("limit_amount", column.name());
        assertFalse(column.nullable());
        assertEquals(19, column.precision());
        assertEquals(2, column.scale());
    }

    @Test
    void shouldHaveCurrencyColumnMapping() throws Exception {
        Column column = fieldAnnotation("currency", Column.class);

        assertEquals("currency", column.name());
        assertFalse(column.nullable());
        assertEquals(3, column.length());
    }

    @Test
    void shouldHaveStatusColumnMapping() throws Exception {
        Enumerated enumerated = fieldAnnotation("status", Enumerated.class);
        Column column = fieldAnnotation("status", Column.class);

        assertEquals(EnumType.STRING, enumerated.value());
        assertEquals("status", column.name());
        assertFalse(column.nullable());
    }

    @Test
    void shouldHaveArchivedAtColumnMapping() throws Exception {
        Column column = fieldAnnotation("archivedAt", Column.class);

        assertEquals("archived_at", column.name());
    }

    @Test
    void shouldHaveNaturalKeyColumnMapping() throws Exception {
        Column column = fieldAnnotation("naturalKey", Column.class);

        assertEquals("natural_key", column.name());
        assertFalse(column.nullable());
        assertEquals(200, column.length());
        assertFalse(column.updatable());
    }

    @Test
    void shouldHaveCreatedAtColumnMapping() throws Exception {
        Column column = fieldAnnotation("createdAt", Column.class);

        assertEquals("created_at", column.name());
        assertFalse(column.nullable());
        assertFalse(column.updatable());
    }

    @Test
    void shouldHaveUpdatedAtColumnMapping() throws Exception {
        Column column = fieldAnnotation("updatedAt", Column.class);

        assertEquals("updated_at", column.name());
        assertFalse(column.nullable());
    }

    @Test
    void shouldHaveLifecycleAnnotations() throws Exception {
        Method prePersist = BudgetEntity.class.getDeclaredMethod("prePersist");
        Method preUpdate = BudgetEntity.class.getDeclaredMethod("preUpdate");

        assertNotNull(prePersist.getAnnotation(PrePersist.class));
        assertNotNull(preUpdate.getAnnotation(PreUpdate.class));
    }

    private static <A extends java.lang.annotation.Annotation> A fieldAnnotation(
            String fieldName,
            Class<A> annotationType
    ) throws Exception {
        return BudgetEntity.class
                .getDeclaredField(fieldName)
                .getAnnotation(annotationType);
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