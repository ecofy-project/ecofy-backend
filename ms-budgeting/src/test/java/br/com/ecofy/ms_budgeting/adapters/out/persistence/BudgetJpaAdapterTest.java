package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetEntity;
import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetRepository;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.BudgetKey;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.CategoryId;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Period;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.UserId;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetsPort;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BudgetJpaAdapterTest {

    private static final UUID ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID SECOND_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID USER_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID CATEGORY_ID =
            UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

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
    void shouldCreateAdapterWithRepository() {
        BudgetRepository repo = mock(BudgetRepository.class);

        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        assertNotNull(adapter);
        assertInstanceOf(SaveBudgetPort.class, adapter);
        assertInstanceOf(LoadBudgetsPort.class, adapter);
    }

    @Test
    void shouldSaveBudgetAndReturnRehydratedDomain() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        Budget budget = budget(
                ID,
                USER_ID,
                CATEGORY_ID,
                PERIOD_START,
                PERIOD_END,
                periodType,
                new BigDecimal("1000.50"),
                status,
                CREATED_AT,
                UPDATED_AT
        );

        when(repo.save(any(BudgetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Budget result = adapter.save(budget);

        ArgumentCaptor<BudgetEntity> entityCaptor =
                ArgumentCaptor.forClass(BudgetEntity.class);

        verify(repo).save(entityCaptor.capture());
        verifyNoMoreInteractions(repo);

        BudgetEntity savedEntity = entityCaptor.getValue();

        assertNotNull(savedEntity);
        assertEquals(ID, savedEntity.getId());
        assertEquals(USER_ID, savedEntity.getUserId());
        assertEquals(CATEGORY_ID, savedEntity.getCategoryId());
        assertEquals(periodType, savedEntity.getPeriodType());
        assertEquals(PERIOD_START, savedEntity.getPeriodStart());
        assertEquals(PERIOD_END, savedEntity.getPeriodEnd());
        assertEquals(new BigDecimal("1000.50"), savedEntity.getLimitAmount());
        assertEquals("BRL", savedEntity.getCurrency());
        assertEquals(status, savedEntity.getStatus());
        assertEquals(naturalKey(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END), savedEntity.getNaturalKey());
        assertEquals(CREATED_AT, savedEntity.getCreatedAt());
        assertEquals(UPDATED_AT, savedEntity.getUpdatedAt());

        assertBudget(
                result,
                ID,
                USER_ID,
                CATEGORY_ID,
                PERIOD_START,
                PERIOD_END,
                periodType,
                new BigDecimal("1000.50"),
                status,
                CREATED_AT,
                UPDATED_AT
        );
    }

    @Test
    void shouldReturnDomainFromEntityReturnedByRepositoryOnSave() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        BudgetPeriodType periodType = anyBudgetPeriodType();

        Budget budget = budget(
                ID,
                USER_ID,
                CATEGORY_ID,
                PERIOD_START,
                PERIOD_END,
                periodType,
                new BigDecimal("1000.50"),
                BudgetStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT
        );

        BudgetEntity persistedEntity = entity(
                SECOND_ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("2000.00"),
                "BRL",
                BudgetStatus.ACTIVE,
                CREATED_AT.plusSeconds(60),
                UPDATED_AT.plusSeconds(60)
        );

        when(repo.save(any(BudgetEntity.class)))
                .thenReturn(persistedEntity);

        Budget result = adapter.save(budget);

        assertBudget(
                result,
                SECOND_ID,
                USER_ID,
                CATEGORY_ID,
                PERIOD_START,
                PERIOD_END,
                periodType,
                new BigDecimal("2000.00"),
                BudgetStatus.ACTIVE,
                CREATED_AT.plusSeconds(60),
                UPDATED_AT.plusSeconds(60)
        );

        verify(repo).save(any(BudgetEntity.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBudgetIsNullOnSave() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.save(null)
        );

        assertEquals("budget must not be null", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldPropagateMapperExceptionBeforeRepositorySave() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        Budget invalidBudget = mock(Budget.class);

        when(invalidBudget.getId()).thenReturn(ID);
        when(invalidBudget.getKey()).thenReturn(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.save(invalidBudget)
        );

        assertEquals("budget.key must not be null", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldPropagateDataIntegrityViolationExceptionWithMostSpecificCauseOnSave() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        Budget budget = validBudget();

        DataIntegrityViolationException repositoryException =
                new DataIntegrityViolationException(
                        "constraint violation",
                        new RuntimeException("duplicate natural key")
                );

        when(repo.save(any(BudgetEntity.class)))
                .thenThrow(repositoryException);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> adapter.save(budget)
        );

        assertSame(repositoryException, exception);
        assertEquals("duplicate natural key", exception.getMostSpecificCause().getMessage());

        verify(repo).save(any(BudgetEntity.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldPropagateDataIntegrityViolationExceptionWithoutMostSpecificCauseOnSave() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        Budget budget = validBudget();

        DataIntegrityViolationException repositoryException =
                mock(DataIntegrityViolationException.class);

        when(repositoryException.getMostSpecificCause()).thenReturn(null);
        when(repositoryException.getMessage()).thenReturn("constraint violation");

        when(repo.save(any(BudgetEntity.class)))
                .thenThrow(repositoryException);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> adapter.save(budget)
        );

        assertSame(repositoryException, exception);

        verify(repo).save(any(BudgetEntity.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldFindByIdWhenBudgetExists() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        BudgetPeriodType periodType = anyBudgetPeriodType();

        BudgetEntity entity = entity(
                ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.50"),
                "BRL",
                BudgetStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT
        );

        when(repo.findById(ID))
                .thenReturn(Optional.of(entity));

        Optional<Budget> result = adapter.findById(ID);

        assertTrue(result.isPresent());

        assertBudget(
                result.get(),
                ID,
                USER_ID,
                CATEGORY_ID,
                PERIOD_START,
                PERIOD_END,
                periodType,
                new BigDecimal("1000.50"),
                BudgetStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT
        );

        verify(repo).findById(ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnEmptyWhenBudgetDoesNotExistById() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        when(repo.findById(ID))
                .thenReturn(Optional.empty());

        Optional<Budget> result = adapter.findById(ID);

        assertTrue(result.isEmpty());

        verify(repo).findById(ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenIdIsNullOnFindById() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.findById(null)
        );

        assertEquals("id must not be null", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldFindBudgetsByUserId() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        BudgetPeriodType periodType = anyBudgetPeriodType();

        BudgetEntity first = entity(
                ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.50"),
                "BRL",
                BudgetStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT
        );

        BudgetEntity second = entity(
                SECOND_ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START.plusMonths(1),
                PERIOD_END.plusMonths(1),
                new BigDecimal("2000.00"),
                "BRL",
                BudgetStatus.ACTIVE,
                CREATED_AT.plusSeconds(60),
                UPDATED_AT.plusSeconds(60)
        );

        when(repo.findByUserId(USER_ID))
                .thenReturn(List.of(first, second));

        List<Budget> result = adapter.findByUserId(USER_ID);

        assertEquals(2, result.size());

        assertEquals(ID, result.get(0).getId());
        assertEquals(SECOND_ID, result.get(1).getId());
        assertEquals(USER_ID, result.get(0).getKey().userId().value());
        assertEquals(USER_ID, result.get(1).getKey().userId().value());

        verify(repo).findByUserId(USER_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryReturnsNullOnFindByUserId() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        when(repo.findByUserId(USER_ID))
                .thenReturn(null);

        List<Budget> result = adapter.findByUserId(USER_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(repo).findByUserId(USER_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryReturnsEmptyListOnFindByUserId() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        when(repo.findByUserId(USER_ID))
                .thenReturn(List.of());

        List<Budget> result = adapter.findByUserId(USER_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(repo).findByUserId(USER_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenUserIdIsNullOnFindByUserId() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.findByUserId(null)
        );

        assertEquals("userId must not be null", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldCheckIfNaturalKeyExistsUsingTrimmedValue() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        String naturalKey = naturalKey(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END);

        when(repo.existsByNaturalKey(naturalKey))
                .thenReturn(true);

        boolean result = adapter.existsByNaturalKey("  " + naturalKey + "  ");

        assertTrue(result);

        verify(repo).existsByNaturalKey(naturalKey);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnFalseWhenNaturalKeyDoesNotExist() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        String naturalKey = naturalKey(USER_ID, CATEGORY_ID, PERIOD_START, PERIOD_END);

        when(repo.existsByNaturalKey(naturalKey))
                .thenReturn(false);

        boolean result = adapter.existsByNaturalKey(naturalKey);

        assertFalse(result);

        verify(repo).existsByNaturalKey(naturalKey);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNaturalKeyIsNullOnExistsByNaturalKey() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.existsByNaturalKey(null)
        );

        assertEquals("naturalKey must not be null", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNaturalKeyIsBlankOnExistsByNaturalKey() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.existsByNaturalKey("   ")
        );

        assertEquals("naturalKey must not be blank", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldFindAllActiveBudgets() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        BudgetPeriodType periodType = anyBudgetPeriodType();

        BudgetEntity first = entity(
                ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.50"),
                "BRL",
                BudgetStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT
        );

        BudgetEntity second = entity(
                SECOND_ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START.plusMonths(1),
                PERIOD_END.plusMonths(1),
                new BigDecimal("2000.00"),
                "BRL",
                BudgetStatus.ACTIVE,
                CREATED_AT.plusSeconds(60),
                UPDATED_AT.plusSeconds(60)
        );

        when(repo.findByStatus(BudgetStatus.ACTIVE))
                .thenReturn(List.of(first, second));

        List<Budget> result = adapter.findAllActive();

        assertEquals(2, result.size());
        assertEquals(ID, result.get(0).getId());
        assertEquals(SECOND_ID, result.get(1).getId());
        assertEquals(BudgetStatus.ACTIVE, result.get(0).getStatus());
        assertEquals(BudgetStatus.ACTIVE, result.get(1).getStatus());

        verify(repo).findByStatus(BudgetStatus.ACTIVE);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryReturnsNullOnFindAllActive() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        when(repo.findByStatus(BudgetStatus.ACTIVE))
                .thenReturn(null);

        List<Budget> result = adapter.findAllActive();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(repo).findByStatus(BudgetStatus.ACTIVE);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryReturnsEmptyListOnFindAllActive() {
        BudgetRepository repo = mock(BudgetRepository.class);
        BudgetJpaAdapter adapter = new BudgetJpaAdapter(repo);

        when(repo.findByStatus(BudgetStatus.ACTIVE))
                .thenReturn(List.of());

        List<Budget> result = adapter.findAllActive();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(repo).findByStatus(BudgetStatus.ACTIVE);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldHaveComponentAnnotation() {
        assertNotNull(BudgetJpaAdapter.class.getAnnotation(Component.class));
    }

    @Test
    void shouldHaveTransactionalAnnotationOnSaveMethod() throws Exception {
        Method method = BudgetJpaAdapter.class.getDeclaredMethod(
                "save",
                Budget.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    @Test
    void shouldHaveReadOnlyTransactionalAnnotationOnFindByIdMethod() throws Exception {
        Method method = BudgetJpaAdapter.class.getDeclaredMethod(
                "findById",
                UUID.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }

    @Test
    void shouldHaveReadOnlyTransactionalAnnotationOnFindByUserIdMethod() throws Exception {
        Method method = BudgetJpaAdapter.class.getDeclaredMethod(
                "findByUserId",
                UUID.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }

    @Test
    void shouldHaveReadOnlyTransactionalAnnotationOnExistsByNaturalKeyMethod() throws Exception {
        Method method = BudgetJpaAdapter.class.getDeclaredMethod(
                "existsByNaturalKey",
                String.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }

    @Test
    void shouldHaveReadOnlyTransactionalAnnotationOnFindAllActiveMethod() throws Exception {
        Method method = BudgetJpaAdapter.class.getDeclaredMethod("findAllActive");

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithValidValue() throws Exception {
        Method method = BudgetJpaAdapter.class.getDeclaredMethod(
                "requireNonBlank",
                String.class,
                String.class
        );

        method.setAccessible(true);

        String result = (String) method.invoke(null, "  value  ", "field");

        assertEquals("value", result);
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithNullValue() throws Exception {
        Method method = BudgetJpaAdapter.class.getDeclaredMethod(
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
        assertEquals("field must not be null", exception.getCause().getMessage());
    }

    @Test
    void shouldInvokePrivateRequireNonBlankWithBlankValue() throws Exception {
        Method method = BudgetJpaAdapter.class.getDeclaredMethod(
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

    private static Budget validBudget() {
        return budget(
                ID,
                USER_ID,
                CATEGORY_ID,
                PERIOD_START,
                PERIOD_END,
                anyBudgetPeriodType(),
                new BigDecimal("1000.50"),
                BudgetStatus.ACTIVE,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static Budget budget(
            UUID id,
            UUID userId,
            UUID categoryId,
            LocalDate periodStart,
            LocalDate periodEnd,
            BudgetPeriodType periodType,
            BigDecimal amount,
            BudgetStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Budget(
                id,
                key(userId, categoryId, periodStart, periodEnd),
                periodType,
                new Money(amount, BRL),
                status,
                createdAt,
                updatedAt
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
                .naturalKey(naturalKey(userId, categoryId, periodStart, periodEnd))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
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

    private static void assertBudget(
            Budget budget,
            UUID expectedId,
            UUID expectedUserId,
            UUID expectedCategoryId,
            LocalDate expectedPeriodStart,
            LocalDate expectedPeriodEnd,
            BudgetPeriodType expectedPeriodType,
            BigDecimal expectedAmount,
            BudgetStatus expectedStatus,
            Instant expectedCreatedAt,
            Instant expectedUpdatedAt
    ) {
        assertNotNull(budget);
        assertEquals(expectedId, budget.getId());
        assertEquals(expectedUserId, budget.getKey().userId().value());
        assertEquals(expectedCategoryId, budget.getKey().categoryId().value());
        assertEquals(expectedPeriodStart, budget.getKey().period().start());
        assertEquals(expectedPeriodEnd, budget.getKey().period().end());
        assertEquals(expectedPeriodType, budget.getPeriodType());
        assertEquals(expectedAmount, budget.getLimit().amount());
        assertEquals(BRL, budget.getLimit().currency());
        assertEquals(expectedStatus, budget.getStatus());
        assertEquals(expectedCreatedAt, budget.getCreatedAt());
        assertEquals(expectedUpdatedAt, budget.getUpdatedAt());
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