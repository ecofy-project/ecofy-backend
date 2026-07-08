package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.application.command.CreateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.DeleteBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.UpdateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidCurrencyCodeException;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidFieldException;
import br.com.ecofy.ms_budgeting.core.application.exception.MissingIdempotencyKeyException;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetResult;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.exception.BudgetAlreadyExistsException;
import br.com.ecofy.ms_budgeting.core.domain.exception.BudgetNotFoundException;
import br.com.ecofy.ms_budgeting.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.BudgetKey;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.CategoryId;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Period;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.UserId;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteBudgetPort;
import br.com.ecofy.ms_budgeting.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetsPort;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    @Mock
    private SaveBudgetPort saveBudgetPort;

    @Mock
    private LoadBudgetsPort loadBudgetsPort;

    @Mock
    private DeleteBudgetPort deleteBudgetPort;

    @Mock
    private IdempotencyPort idempotencyPort;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BudgetingProperties props;

    private BudgetCommandService service;

    @BeforeEach
    void setUp() {
        service = new BudgetCommandService(
                saveBudgetPort,
                loadBudgetsPort,
                deleteBudgetPort,
                idempotencyPort,
                props,
                CLOCK
        );

        when(props.idempotency().ttl()).thenReturn(IDEMPOTENCY_TTL);
    }

    @Test
    void shouldCreateBudgetSuccessfullyWithDefaultStatusWhenStatusIsNull() {
        CreateBudgetCommand command = createCommand(null);

        when(idempotencyPort.tryAcquire("key-create", IDEMPOTENCY_TTL, "api:budget:create"))
                .thenReturn(true);
        when(loadBudgetsPort.existsByNaturalKey(anyString()))
                .thenReturn(false);
        when(saveBudgetPort.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResult result = service.create(command, " key-create ");

        assertNotNull(result.id());
        assertEquals(command.userId(), result.userId());
        assertEquals(command.categoryId(), result.categoryId());
        assertEquals(command.periodType(), result.periodType());
        assertEquals(command.periodStart(), result.periodStart());
        assertEquals(command.periodEnd(), result.periodEnd());
        assertEquals(command.limitAmount(), result.limitAmount());
        assertEquals("BRL", result.currency());
        assertEquals(BudgetStatus.ACTIVE, result.status());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW, result.updatedAt());

        verify(idempotencyPort).tryAcquire("key-create", IDEMPOTENCY_TTL, "api:budget:create");
        verify(loadBudgetsPort).existsByNaturalKey(anyString());
        verify(saveBudgetPort).save(any(Budget.class));
    }

    @Test
    void shouldCreateBudgetSuccessfullyWithProvidedStatusAndTrimmedCurrency() {
        CreateBudgetCommand command = new CreateBudgetCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BudgetPeriodType.values()[0],
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                new BigDecimal("500.00"),
                " BRL ",
                BudgetStatus.ACTIVE
        );

        when(idempotencyPort.tryAcquire("key-create", IDEMPOTENCY_TTL, "api:budget:create"))
                .thenReturn(true);
        when(loadBudgetsPort.existsByNaturalKey(anyString()))
                .thenReturn(false);
        when(saveBudgetPort.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResult result = service.create(command, "key-create");

        assertEquals("BRL", result.currency());
        assertEquals(BudgetStatus.ACTIVE, result.status());

        verify(saveBudgetPort).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenCreateCommandIsNull() {
        assertThrows(
                InvalidFieldException.class,
                () -> service.create(null, "key-create")
        );

        verify(idempotencyPort, never()).tryAcquire(anyString(), any(), anyString());
        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenCreateIdempotencyKeyIsNull() {
        CreateBudgetCommand command = createCommand(BudgetStatus.ACTIVE);

        assertThrows(
                MissingIdempotencyKeyException.class,
                () -> service.create(command, null)
        );

        verify(idempotencyPort, never()).tryAcquire(anyString(), any(), anyString());
        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenCreateIdempotencyKeyIsBlank() {
        CreateBudgetCommand command = createCommand(BudgetStatus.ACTIVE);

        assertThrows(
                MissingIdempotencyKeyException.class,
                () -> service.create(command, "   ")
        );

        verify(idempotencyPort, never()).tryAcquire(anyString(), any(), anyString());
        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenCreateIdempotencyKeyWasAlreadyAcquired() {
        CreateBudgetCommand command = createCommand(BudgetStatus.ACTIVE);

        when(idempotencyPort.tryAcquire("key-create", IDEMPOTENCY_TTL, "api:budget:create"))
                .thenReturn(false);

        assertThrows(
                IdempotencyViolationException.class,
                () -> service.create(command, "key-create")
        );

        verify(idempotencyPort).tryAcquire("key-create", IDEMPOTENCY_TTL, "api:budget:create");
        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenBudgetAlreadyExistsOnCreate() {
        CreateBudgetCommand command = createCommand(BudgetStatus.ACTIVE);

        when(idempotencyPort.tryAcquire("key-create", IDEMPOTENCY_TTL, "api:budget:create"))
                .thenReturn(true);
        when(loadBudgetsPort.existsByNaturalKey(anyString()))
                .thenReturn(true);

        assertThrows(
                BudgetAlreadyExistsException.class,
                () -> service.create(command, "key-create")
        );

        verify(loadBudgetsPort).existsByNaturalKey(anyString());
        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenCreatePeriodStartIsAfterPeriodEnd() {
        CreateBudgetCommand command = new CreateBudgetCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BudgetPeriodType.values()[0],
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 31),
                new BigDecimal("1000.00"),
                "BRL",
                BudgetStatus.ACTIVE
        );

        when(idempotencyPort.tryAcquire("key-create", IDEMPOTENCY_TTL, "api:budget:create"))
                .thenReturn(true);

        assertThrows(
                InvalidFieldException.class,
                () -> service.create(command, "key-create")
        );

        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenCreateCurrencyIsBlank() {
        CreateBudgetCommand command = new CreateBudgetCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BudgetPeriodType.values()[0],
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                new BigDecimal("1000.00"),
                " ",
                BudgetStatus.ACTIVE
        );

        when(idempotencyPort.tryAcquire("key-create", IDEMPOTENCY_TTL, "api:budget:create"))
                .thenReturn(true);

        assertThrows(
                InvalidFieldException.class,
                () -> service.create(command, "key-create")
        );

        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenCreateCurrencyIsInvalid() {
        CreateBudgetCommand command = new CreateBudgetCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BudgetPeriodType.values()[0],
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                new BigDecimal("1000.00"),
                "INVALID",
                BudgetStatus.ACTIVE
        );

        when(idempotencyPort.tryAcquire("key-create", IDEMPOTENCY_TTL, "api:budget:create"))
                .thenReturn(true);

        assertThrows(
                InvalidCurrencyCodeException.class,
                () -> service.create(command, "key-create")
        );

        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldUpdateBudgetLimitCurrencyAndStatusSuccessfully() {
        UUID budgetId = UUID.randomUUID();
        Budget budget = budget(budgetId);

        UpdateBudgetCommand command = new UpdateBudgetCommand(
                budgetId,
                new BigDecimal("2000.00"),
                "USD",
                BudgetStatus.ACTIVE
        );

        when(idempotencyPort.tryAcquire("key-update", IDEMPOTENCY_TTL, "api:budget:update"))
                .thenReturn(true);
        when(loadBudgetsPort.findById(budgetId))
                .thenReturn(Optional.of(budget));
        when(saveBudgetPort.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResult result = service.update(command, "key-update");

        assertEquals(budgetId, result.id());
        assertEquals(new BigDecimal("2000.00"), result.limitAmount());
        assertEquals("USD", result.currency());
        assertEquals(BudgetStatus.ACTIVE, result.status());
        assertEquals(NOW, result.updatedAt());

        verify(idempotencyPort).tryAcquire("key-update", IDEMPOTENCY_TTL, "api:budget:update");
        verify(loadBudgetsPort).findById(budgetId);
        verify(saveBudgetPort).save(budget);
    }

    @Test
    void shouldUpdateBudgetLimitUsingCurrentCurrencyWhenCurrencyIsNull() {
        UUID budgetId = UUID.randomUUID();
        Budget budget = budget(budgetId);

        UpdateBudgetCommand command = new UpdateBudgetCommand(
                budgetId,
                new BigDecimal("1800.00"),
                null,
                null
        );

        when(idempotencyPort.tryAcquire("key-update", IDEMPOTENCY_TTL, "api:budget:update"))
                .thenReturn(true);
        when(loadBudgetsPort.findById(budgetId))
                .thenReturn(Optional.of(budget));
        when(saveBudgetPort.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResult result = service.update(command, "key-update");

        assertEquals(new BigDecimal("1800.00"), result.limitAmount());
        assertEquals("BRL", result.currency());
        assertEquals(NOW, result.updatedAt());

        verify(saveBudgetPort).save(budget);
    }

    @Test
    void shouldReturnCurrentBudgetWhenUpdateHasNoChanges() {
        UUID budgetId = UUID.randomUUID();
        Budget budget = budget(budgetId);

        UpdateBudgetCommand command = new UpdateBudgetCommand(
                budgetId,
                null,
                null,
                null
        );

        when(idempotencyPort.tryAcquire("key-update", IDEMPOTENCY_TTL, "api:budget:update"))
                .thenReturn(true);
        when(loadBudgetsPort.findById(budgetId))
                .thenReturn(Optional.of(budget));

        BudgetResult result = service.update(command, "key-update");

        assertEquals(budgetId, result.id());
        assertEquals(new BigDecimal("1000.00"), result.limitAmount());
        assertEquals("BRL", result.currency());

        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenUpdateCommandIsNull() {
        assertThrows(
                InvalidFieldException.class,
                () -> service.update(null, "key-update")
        );

        verify(idempotencyPort, never()).tryAcquire(anyString(), any(), anyString());
        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenUpdateBudgetIdIsNull() {
        UpdateBudgetCommand command = new UpdateBudgetCommand(
                null,
                new BigDecimal("2000.00"),
                "BRL",
                BudgetStatus.ACTIVE
        );

        when(idempotencyPort.tryAcquire("key-update", IDEMPOTENCY_TTL, "api:budget:update"))
                .thenReturn(true);

        assertThrows(
                InvalidFieldException.class,
                () -> service.update(command, "key-update")
        );

        verify(loadBudgetsPort, never()).findById(any(UUID.class));
        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenUpdateBudgetDoesNotExist() {
        UUID budgetId = UUID.randomUUID();

        UpdateBudgetCommand command = new UpdateBudgetCommand(
                budgetId,
                new BigDecimal("2000.00"),
                "BRL",
                BudgetStatus.ACTIVE
        );

        when(idempotencyPort.tryAcquire("key-update", IDEMPOTENCY_TTL, "api:budget:update"))
                .thenReturn(true);
        when(loadBudgetsPort.findById(budgetId))
                .thenReturn(Optional.empty());

        assertThrows(
                BudgetNotFoundException.class,
                () -> service.update(command, "key-update")
        );

        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenUpdateCurrencyIsProvidedWithoutNewLimitAmount() {
        UUID budgetId = UUID.randomUUID();
        Budget budget = budget(budgetId);

        UpdateBudgetCommand command = new UpdateBudgetCommand(
                budgetId,
                null,
                "USD",
                null
        );

        when(idempotencyPort.tryAcquire("key-update", IDEMPOTENCY_TTL, "api:budget:update"))
                .thenReturn(true);
        when(loadBudgetsPort.findById(budgetId))
                .thenReturn(Optional.of(budget));

        assertThrows(
                InvalidFieldException.class,
                () -> service.update(command, "key-update")
        );

        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenUpdateCurrencyIsInvalid() {
        UUID budgetId = UUID.randomUUID();
        Budget budget = budget(budgetId);

        UpdateBudgetCommand command = new UpdateBudgetCommand(
                budgetId,
                new BigDecimal("2000.00"),
                "INVALID",
                null
        );

        when(idempotencyPort.tryAcquire("key-update", IDEMPOTENCY_TTL, "api:budget:update"))
                .thenReturn(true);
        when(loadBudgetsPort.findById(budgetId))
                .thenReturn(Optional.of(budget));

        assertThrows(
                InvalidCurrencyCodeException.class,
                () -> service.update(command, "key-update")
        );

        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldThrowWhenUpdateIdempotencyKeyWasAlreadyAcquired() {
        UpdateBudgetCommand command = new UpdateBudgetCommand(
                UUID.randomUUID(),
                new BigDecimal("2000.00"),
                "BRL",
                BudgetStatus.ACTIVE
        );

        when(idempotencyPort.tryAcquire("key-update", IDEMPOTENCY_TTL, "api:budget:update"))
                .thenReturn(false);

        assertThrows(
                IdempotencyViolationException.class,
                () -> service.update(command, "key-update")
        );

        verify(loadBudgetsPort, never()).findById(any(UUID.class));
        verify(saveBudgetPort, never()).save(any(Budget.class));
    }

    @Test
    void shouldDeleteBudgetSuccessfully() {
        UUID budgetId = UUID.randomUUID();
        DeleteBudgetCommand command = new DeleteBudgetCommand(budgetId);

        when(idempotencyPort.tryAcquire("key-delete", IDEMPOTENCY_TTL, "api:budget:delete"))
                .thenReturn(true);
        when(deleteBudgetPort.existsById(budgetId))
                .thenReturn(true);

        service.delete(command, "key-delete");

        verify(idempotencyPort).tryAcquire("key-delete", IDEMPOTENCY_TTL, "api:budget:delete");
        verify(deleteBudgetPort).existsById(budgetId);
        verify(deleteBudgetPort).deleteById(budgetId);
    }

    @Test
    void shouldThrowWhenDeleteCommandIsNull() {
        assertThrows(
                InvalidFieldException.class,
                () -> service.delete(null, "key-delete")
        );

        verify(idempotencyPort, never()).tryAcquire(anyString(), any(), anyString());
        verify(deleteBudgetPort, never()).deleteById(any(UUID.class));
    }

    @Test
    void shouldThrowWhenDeleteBudgetIdIsNull() {
        DeleteBudgetCommand command = new DeleteBudgetCommand(null);

        when(idempotencyPort.tryAcquire("key-delete", IDEMPOTENCY_TTL, "api:budget:delete"))
                .thenReturn(true);

        assertThrows(
                InvalidFieldException.class,
                () -> service.delete(command, "key-delete")
        );

        verify(deleteBudgetPort, never()).existsById(any(UUID.class));
        verify(deleteBudgetPort, never()).deleteById(any(UUID.class));
    }

    @Test
    void shouldThrowWhenDeleteBudgetDoesNotExist() {
        UUID budgetId = UUID.randomUUID();
        DeleteBudgetCommand command = new DeleteBudgetCommand(budgetId);

        when(idempotencyPort.tryAcquire("key-delete", IDEMPOTENCY_TTL, "api:budget:delete"))
                .thenReturn(true);
        when(deleteBudgetPort.existsById(budgetId))
                .thenReturn(false);

        assertThrows(
                BudgetNotFoundException.class,
                () -> service.delete(command, "key-delete")
        );

        verify(deleteBudgetPort).existsById(budgetId);
        verify(deleteBudgetPort, never()).deleteById(any(UUID.class));
    }

    @Test
    void shouldThrowWhenDeleteIdempotencyKeyWasAlreadyAcquired() {
        DeleteBudgetCommand command = new DeleteBudgetCommand(UUID.randomUUID());

        when(idempotencyPort.tryAcquire("key-delete", IDEMPOTENCY_TTL, "api:budget:delete"))
                .thenReturn(false);

        assertThrows(
                IdempotencyViolationException.class,
                () -> service.delete(command, "key-delete")
        );

        verify(deleteBudgetPort, never()).existsById(any(UUID.class));
        verify(deleteBudgetPort, never()).deleteById(any(UUID.class));
    }

    private static CreateBudgetCommand createCommand(BudgetStatus status) {
        return new CreateBudgetCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BudgetPeriodType.values()[0],
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                new BigDecimal("1000.00"),
                "BRL",
                status
        );
    }

    private static Budget budget(UUID budgetId) {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        BudgetKey key = new BudgetKey(
                new UserId(userId),
                new CategoryId(categoryId),
                new Period(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)
                )
        );

        return new Budget(
                budgetId,
                key,
                BudgetPeriodType.values()[0],
                new Money(new BigDecimal("1000.00"), Currency.getInstance("BRL")),
                BudgetStatus.ACTIVE,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }
}