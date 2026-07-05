package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetConsumptionEntity;
import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetConsumptionRepository;
import br.com.ecofy.ms_budgeting.core.domain.BudgetConsumption;
import br.com.ecofy.ms_budgeting.core.domain.enums.ConsumptionSource;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetConsumptionPort;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetConsumptionPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BudgetConsumptionJpaAdapterTest {

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
    void shouldCreateAdapterWithRepository() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        assertNotNull(adapter);
        assertInstanceOf(SaveBudgetConsumptionPort.class, adapter);
        assertInstanceOf(LoadBudgetConsumptionPort.class, adapter);
    }

    @Test
    void shouldSaveBudgetConsumptionAndReturnRehydratedDomain() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        ConsumptionSource source = anyConsumptionSource();

        BudgetConsumption consumption = domain(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("123.45"),
                source,
                CREATED_AT,
                UPDATED_AT
        );

        when(repository.save(any(BudgetConsumptionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetConsumption result = adapter.save(consumption);

        ArgumentCaptor<BudgetConsumptionEntity> entityCaptor =
                ArgumentCaptor.forClass(BudgetConsumptionEntity.class);

        verify(repository).save(entityCaptor.capture());
        verifyNoMoreInteractions(repository);

        BudgetConsumptionEntity savedEntity = entityCaptor.getValue();

        assertNotNull(savedEntity);
        assertEquals(ID, savedEntity.getId());
        assertEquals(BUDGET_ID, savedEntity.getBudgetId());
        assertEquals(PERIOD_START, savedEntity.getPeriodStart());
        assertEquals(PERIOD_END, savedEntity.getPeriodEnd());
        assertEquals(12345L, savedEntity.getConsumedCents());
        assertEquals("BRL", savedEntity.getCurrency());
        assertEquals(source.name(), savedEntity.getSource());
        assertEquals(CREATED_AT, savedEntity.getCreatedAt());
        assertEquals(UPDATED_AT, savedEntity.getUpdatedAt());

        assertNotNull(result);
        assertEquals(ID, result.getId());
        assertEquals(BUDGET_ID, result.getBudgetId());
        assertEquals(PERIOD_START, result.getPeriodStart());
        assertEquals(PERIOD_END, result.getPeriodEnd());
        assertEquals(new BigDecimal("123.45"), result.getConsumed().amount());
        assertEquals(BRL, result.getConsumed().currency());
        assertEquals(source, result.getSource());
        assertEquals(CREATED_AT, result.getCreatedAt());
        assertEquals(UPDATED_AT, result.getUpdatedAt());
    }

    @Test
    void shouldReturnDomainFromEntityReturnedByRepository() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        ConsumptionSource source = anyConsumptionSource();

        BudgetConsumption consumption = domain(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("50.00"),
                source,
                CREATED_AT,
                UPDATED_AT
        );

        UUID persistedId =
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        Instant persistedCreatedAt =
                Instant.parse("2026-06-26T10:30:00Z");

        Instant persistedUpdatedAt =
                Instant.parse("2026-06-26T11:30:00Z");

        BudgetConsumptionEntity persistedEntity = entity(
                persistedId,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                98765L,
                "BRL",
                source.name(),
                persistedCreatedAt,
                persistedUpdatedAt
        );

        when(repository.save(any(BudgetConsumptionEntity.class)))
                .thenReturn(persistedEntity);

        BudgetConsumption result = adapter.save(consumption);

        assertNotNull(result);
        assertEquals(persistedId, result.getId());
        assertEquals(BUDGET_ID, result.getBudgetId());
        assertEquals(PERIOD_START, result.getPeriodStart());
        assertEquals(PERIOD_END, result.getPeriodEnd());
        assertEquals(new BigDecimal("987.65"), result.getConsumed().amount());
        assertEquals(BRL, result.getConsumed().currency());
        assertEquals(source, result.getSource());
        assertEquals(persistedCreatedAt, result.getCreatedAt());
        assertEquals(persistedUpdatedAt, result.getUpdatedAt());

        verify(repository).save(any(BudgetConsumptionEntity.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenConsumptionIsNullOnSave() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.save(null)
        );

        assertEquals("consumption must not be null", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldPropagateExceptionWhenMapperFailsBeforeRepositorySave() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        BudgetConsumption invalidConsumption = mock(BudgetConsumption.class);

        when(invalidConsumption.getBudgetId()).thenReturn(BUDGET_ID);
        when(invalidConsumption.getPeriodStart()).thenReturn(PERIOD_START);
        when(invalidConsumption.getPeriodEnd()).thenReturn(PERIOD_END);
        when(invalidConsumption.getSource()).thenReturn(anyConsumptionSource());
        when(invalidConsumption.getConsumed()).thenReturn(null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.save(invalidConsumption)
        );

        assertEquals("domain.consumed must not be null", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldPropagateRepositoryExceptionOnSave() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        BudgetConsumption consumption = validConsumption();

        RuntimeException repositoryException =
                new RuntimeException("database unavailable");

        when(repository.save(any(BudgetConsumptionEntity.class)))
                .thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.save(consumption)
        );

        assertSame(repositoryException, exception);

        verify(repository).save(any(BudgetConsumptionEntity.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldFindByBudgetAndPeriodWhenEntityExists() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        ConsumptionSource source = anyConsumptionSource();

        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                12345L,
                "BRL",
                source.name(),
                CREATED_AT,
                UPDATED_AT
        );

        when(repository.findByBudgetIdAndPeriodStartAndPeriodEnd(
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END
        )).thenReturn(Optional.of(entity));

        Optional<BudgetConsumption> result =
                adapter.findByBudgetAndPeriod(BUDGET_ID, PERIOD_START, PERIOD_END);

        assertTrue(result.isPresent());

        BudgetConsumption consumption = result.get();

        assertEquals(ID, consumption.getId());
        assertEquals(BUDGET_ID, consumption.getBudgetId());
        assertEquals(PERIOD_START, consumption.getPeriodStart());
        assertEquals(PERIOD_END, consumption.getPeriodEnd());
        assertEquals(new BigDecimal("123.45"), consumption.getConsumed().amount());
        assertEquals(BRL, consumption.getConsumed().currency());
        assertEquals(source, consumption.getSource());
        assertEquals(CREATED_AT, consumption.getCreatedAt());
        assertEquals(UPDATED_AT, consumption.getUpdatedAt());

        verify(repository).findByBudgetIdAndPeriodStartAndPeriodEnd(
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END
        );
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyWhenBudgetAndPeriodDoesNotExist() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        when(repository.findByBudgetIdAndPeriodStartAndPeriodEnd(
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END
        )).thenReturn(Optional.empty());

        Optional<BudgetConsumption> result =
                adapter.findByBudgetAndPeriod(BUDGET_ID, PERIOD_START, PERIOD_END);

        assertTrue(result.isEmpty());

        verify(repository).findByBudgetIdAndPeriodStartAndPeriodEnd(
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END
        );
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBudgetIdIsNullOnFindByBudgetAndPeriod() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.findByBudgetAndPeriod(null, PERIOD_START, PERIOD_END)
        );

        assertEquals("budgetId must not be null", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenStartIsNullOnFindByBudgetAndPeriod() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.findByBudgetAndPeriod(BUDGET_ID, null, PERIOD_END)
        );

        assertEquals("start must not be null", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEndIsNullOnFindByBudgetAndPeriod() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.findByBudgetAndPeriod(BUDGET_ID, PERIOD_START, null)
        );

        assertEquals("end must not be null", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenStartIsAfterEndOnFindByBudgetAndPeriod() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.findByBudgetAndPeriod(BUDGET_ID, start, end)
        );

        assertEquals("start must be <= end", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldAcceptSameStartAndEndOnFindByBudgetAndPeriod() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        LocalDate sameDay = LocalDate.of(2026, 6, 1);

        when(repository.findByBudgetIdAndPeriodStartAndPeriodEnd(
                BUDGET_ID,
                sameDay,
                sameDay
        )).thenReturn(Optional.empty());

        Optional<BudgetConsumption> result =
                adapter.findByBudgetAndPeriod(BUDGET_ID, sameDay, sameDay);

        assertTrue(result.isEmpty());

        verify(repository).findByBudgetIdAndPeriodStartAndPeriodEnd(
                BUDGET_ID,
                sameDay,
                sameDay
        );
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldFindLatestByBudgetIdWhenEntityExists() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        ConsumptionSource source = anyConsumptionSource();

        BudgetConsumptionEntity entity = entity(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                20000L,
                "BRL",
                source.name(),
                CREATED_AT,
                UPDATED_AT
        );

        when(repository.findTopByBudgetIdOrderByUpdatedAtDesc(BUDGET_ID))
                .thenReturn(Optional.of(entity));

        Optional<BudgetConsumption> result =
                adapter.findLatestByBudgetId(BUDGET_ID);

        assertTrue(result.isPresent());

        BudgetConsumption consumption = result.get();

        assertEquals(ID, consumption.getId());
        assertEquals(BUDGET_ID, consumption.getBudgetId());
        assertEquals(PERIOD_START, consumption.getPeriodStart());
        assertEquals(PERIOD_END, consumption.getPeriodEnd());
        assertEquals(new BigDecimal("200.00"), consumption.getConsumed().amount());
        assertEquals(BRL, consumption.getConsumed().currency());
        assertEquals(source, consumption.getSource());
        assertEquals(CREATED_AT, consumption.getCreatedAt());
        assertEquals(UPDATED_AT, consumption.getUpdatedAt());

        verify(repository).findTopByBudgetIdOrderByUpdatedAtDesc(BUDGET_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnEmptyWhenLatestByBudgetIdDoesNotExist() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        when(repository.findTopByBudgetIdOrderByUpdatedAtDesc(BUDGET_ID))
                .thenReturn(Optional.empty());

        Optional<BudgetConsumption> result =
                adapter.findLatestByBudgetId(BUDGET_ID);

        assertTrue(result.isEmpty());

        verify(repository).findTopByBudgetIdOrderByUpdatedAtDesc(BUDGET_ID);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenBudgetIdIsNullOnFindLatestByBudgetId() {
        BudgetConsumptionRepository repository = mock(BudgetConsumptionRepository.class);

        BudgetConsumptionJpaAdapter adapter =
                new BudgetConsumptionJpaAdapter(repository);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.findLatestByBudgetId(null)
        );

        assertEquals("budgetId must not be null", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldHaveComponentAnnotation() {
        assertNotNull(BudgetConsumptionJpaAdapter.class.getAnnotation(Component.class));
    }

    @Test
    void shouldHaveTransactionalAnnotationOnSaveMethod() throws Exception {
        Method method = BudgetConsumptionJpaAdapter.class.getDeclaredMethod(
                "save",
                BudgetConsumption.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    @Test
    void shouldHaveReadOnlyTransactionalAnnotationOnFindByBudgetAndPeriodMethod() throws Exception {
        Method method = BudgetConsumptionJpaAdapter.class.getDeclaredMethod(
                "findByBudgetAndPeriod",
                UUID.class,
                LocalDate.class,
                LocalDate.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }

    @Test
    void shouldHaveReadOnlyTransactionalAnnotationOnFindLatestByBudgetIdMethod() throws Exception {
        Method method = BudgetConsumptionJpaAdapter.class.getDeclaredMethod(
                "findLatestByBudgetId",
                UUID.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }

    private static BudgetConsumption validConsumption() {
        return domain(
                ID,
                BUDGET_ID,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("123.45"),
                anyConsumptionSource(),
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static BudgetConsumption domain(
            UUID id,
            UUID budgetId,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal amount,
            ConsumptionSource source,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new BudgetConsumption(
                id,
                budgetId,
                periodStart,
                periodEnd,
                new Money(amount, BRL),
                source,
                createdAt,
                updatedAt
        );
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

    private static ConsumptionSource anyConsumptionSource() {
        ConsumptionSource[] values = ConsumptionSource.values();

        if (values.length == 0) {
            throw new IllegalStateException("ConsumptionSource enum must have at least one value");
        }

        return values[0];
    }
}