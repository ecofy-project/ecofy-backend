package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.application.command.ProcessTransactionCommand;
import br.com.ecofy.ms_budgeting.core.application.exception.BudgetProjectionProcessingException;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidCurrencyCodeException;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidFieldException;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.domain.BudgetConsumption;
import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.enums.ConsumptionSource;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.BudgetKey;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.CategoryId;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Period;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.UserId;
import br.com.ecofy.ms_budgeting.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetConsumptionPort;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetsPort;
import br.com.ecofy.ms_budgeting.core.port.out.PublishBudgetAlertEventPort;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetAlertPort;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetConsumptionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetProjectionServiceTest {

    private static final String SCOPE_KAFKA_CATEGORIZED_TX = "kafka:categorizedTx";
    private static final String SCOPE_BUDGET_ALERT = "budget:alert";

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 1, 31);
    private static final LocalDate TX_DATE = LocalDate.of(2026, 1, 15);

    @Mock
    private LoadBudgetsPort loadBudgetsPort;

    @Mock
    private LoadBudgetConsumptionPort loadBudgetConsumptionPort;

    @Mock
    private SaveBudgetConsumptionPort saveBudgetConsumptionPort;

    @Mock
    private SaveBudgetAlertPort saveBudgetAlertPort;

    @Mock
    private PublishBudgetAlertEventPort publishBudgetAlertEventPort;

    @Mock
    private IdempotencyPort idempotencyPort;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BudgetingProperties props;

    private BudgetProjectionService service;

    @BeforeEach
    void setUp() {
        service = new BudgetProjectionService(
                loadBudgetsPort,
                loadBudgetConsumptionPort,
                saveBudgetConsumptionPort,
                saveBudgetAlertPort,
                publishBudgetAlertEventPort,
                idempotencyPort,
                props,
                CLOCK
        );

        lenient().when(props.idempotency().ttl()).thenReturn(IDEMPOTENCY_TTL);
        lenient().when(props.alerts().warningThresholdPct()).thenReturn(new BigDecimal("80.00"));
        lenient().when(props.alerts().criticalThresholdPct()).thenReturn(new BigDecimal("100.00"));
        lenient().when(props.alerts().publishOnEveryUpdate()).thenReturn(false);
    }

    @Test
    void shouldThrowWhenConstructorDependencyIsNull() {
        assertThrows(NullPointerException.class, () -> new BudgetProjectionService(
                null,
                loadBudgetConsumptionPort,
                saveBudgetConsumptionPort,
                saveBudgetAlertPort,
                publishBudgetAlertEventPort,
                idempotencyPort,
                props,
                CLOCK
        ));

        assertThrows(NullPointerException.class, () -> new BudgetProjectionService(
                loadBudgetsPort,
                null,
                saveBudgetConsumptionPort,
                saveBudgetAlertPort,
                publishBudgetAlertEventPort,
                idempotencyPort,
                props,
                CLOCK
        ));

        assertThrows(NullPointerException.class, () -> new BudgetProjectionService(
                loadBudgetsPort,
                loadBudgetConsumptionPort,
                null,
                saveBudgetAlertPort,
                publishBudgetAlertEventPort,
                idempotencyPort,
                props,
                CLOCK
        ));

        assertThrows(NullPointerException.class, () -> new BudgetProjectionService(
                loadBudgetsPort,
                loadBudgetConsumptionPort,
                saveBudgetConsumptionPort,
                null,
                publishBudgetAlertEventPort,
                idempotencyPort,
                props,
                CLOCK
        ));

        assertThrows(NullPointerException.class, () -> new BudgetProjectionService(
                loadBudgetsPort,
                loadBudgetConsumptionPort,
                saveBudgetConsumptionPort,
                saveBudgetAlertPort,
                null,
                idempotencyPort,
                props,
                CLOCK
        ));

        assertThrows(NullPointerException.class, () -> new BudgetProjectionService(
                loadBudgetsPort,
                loadBudgetConsumptionPort,
                saveBudgetConsumptionPort,
                saveBudgetAlertPort,
                publishBudgetAlertEventPort,
                null,
                props,
                CLOCK
        ));

        assertThrows(NullPointerException.class, () -> new BudgetProjectionService(
                loadBudgetsPort,
                loadBudgetConsumptionPort,
                saveBudgetConsumptionPort,
                saveBudgetAlertPort,
                publishBudgetAlertEventPort,
                idempotencyPort,
                null,
                CLOCK
        ));

        assertThrows(NullPointerException.class, () -> new BudgetProjectionService(
                loadBudgetsPort,
                loadBudgetConsumptionPort,
                saveBudgetConsumptionPort,
                saveBudgetAlertPort,
                publishBudgetAlertEventPort,
                idempotencyPort,
                props,
                null
        ));
    }

    @Test
    void shouldReturnWhenIdempotencyKeyAlreadyExistsUsingEventId() {
        UUID transactionId = UUID.randomUUID();
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                " event-123 "
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-123",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(false);

        service.process(command);

        verify(idempotencyPort).tryAcquire(
                "kafka:categorized-tx:event:event-123",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        );
        verify(loadBudgetsPort, never()).findByUserId(any(UUID.class));
        verify(saveBudgetConsumptionPort, never()).save(any(BudgetConsumption.class));
    }

    @Test
    void shouldReturnWhenUserHasNoBudgetsUsingTransactionIdFallback() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                null
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:tx:" + transactionId,
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of());

        service.process(command);

        verify(loadBudgetsPort).findByUserId(userId);
        verify(saveBudgetConsumptionPort, never()).save(any(BudgetConsumption.class));
        verify(saveBudgetAlertPort, never()).save(any(BudgetAlert.class));
        verify(publishBudgetAlertEventPort, never()).publish(any(BudgetAlert.class));
    }

    @Test
    void shouldUseTransactionIdFallbackWhenEventIdIsBlank() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                "   "
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:tx:" + transactionId,
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of());

        service.process(command);

        verify(idempotencyPort).tryAcquire(
                "kafka:categorized-tx:tx:" + transactionId,
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        );
    }

    @Test
    void shouldIgnoreBudgetsThatAreNotEligible() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("100.00"),
                "brl",
                TX_DATE,
                "event-ignored"
        );

        Budget inactiveBudget = budget(
                UUID.randomUUID(),
                userId,
                categoryId,
                BudgetStatus.PAUSED,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        Budget differentCategoryBudget = budget(
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        Budget outsidePeriodBudget = budget(
                UUID.randomUUID(),
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                new BigDecimal("1000.00")
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-ignored",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId))
                .thenReturn(List.of(inactiveBudget, differentCategoryBudget, outsidePeriodBudget));

        service.process(command);

        verify(saveBudgetConsumptionPort, never()).save(any(BudgetConsumption.class));
        verify(saveBudgetAlertPort, never()).save(any(BudgetAlert.class));
        verify(publishBudgetAlertEventPort, never()).publish(any(BudgetAlert.class));
    }

    @Test
    void shouldCreateConsumptionWithoutAlertWhenConsumptionIsBelowThreshold() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("100.00"),
                " BRL ",
                TX_DATE,
                "event-below-threshold"
        );

        Budget budget = budget(
                budgetId,
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-below-threshold",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget));
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, PERIOD_START, PERIOD_END))
                .thenReturn(Optional.empty());
        when(saveBudgetConsumptionPort.save(any(BudgetConsumption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.process(command);

        ArgumentCaptor<BudgetConsumption> consumptionCaptor = ArgumentCaptor.forClass(BudgetConsumption.class);

        verify(saveBudgetConsumptionPort).save(consumptionCaptor.capture());
        assertEquals(budgetId, consumptionCaptor.getValue().getBudgetId());
        assertEquals(new BigDecimal("100.00"), consumptionCaptor.getValue().getConsumed().amount());

        verify(saveBudgetAlertPort, never()).save(any(BudgetAlert.class));
        verify(publishBudgetAlertEventPort, never()).publish(any(BudgetAlert.class));
    }

    @Test
    void shouldOnlyLogWhenBelowThresholdAndPublishOnEveryUpdateIsEnabled() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                "event-log-only"
        );

        Budget budget = budget(
                budgetId,
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        when(props.alerts().publishOnEveryUpdate()).thenReturn(true);
        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-log-only",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget));
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, PERIOD_START, PERIOD_END))
                .thenReturn(Optional.empty());
        when(saveBudgetConsumptionPort.save(any(BudgetConsumption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.process(command);

        verify(saveBudgetConsumptionPort).save(any(BudgetConsumption.class));
        verify(saveBudgetAlertPort, never()).save(any(BudgetAlert.class));
        verify(publishBudgetAlertEventPort, never()).publish(any(BudgetAlert.class));
    }

    @Test
    void shouldCreateWarningAlertWhenThresholdIsCrossed() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID consumptionId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                "event-warning"
        );

        Budget budget = budget(
                budgetId,
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        BudgetConsumption existingConsumption = consumption(
                consumptionId,
                budgetId,
                new BigDecimal("700.00")
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-warning",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(idempotencyPort.tryAcquire(
                "alert:" + budgetId + ":" + consumptionId + ":WARNING",
                IDEMPOTENCY_TTL,
                SCOPE_BUDGET_ALERT
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget));
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, PERIOD_START, PERIOD_END))
                .thenReturn(Optional.of(existingConsumption));
        when(saveBudgetConsumptionPort.save(existingConsumption)).thenReturn(existingConsumption);
        when(saveBudgetAlertPort.save(any(BudgetAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.process(command);

        ArgumentCaptor<BudgetAlert> alertCaptor = ArgumentCaptor.forClass(BudgetAlert.class);

        verify(saveBudgetAlertPort).save(alertCaptor.capture());
        verify(publishBudgetAlertEventPort).publish(alertCaptor.getValue());

        BudgetAlert alert = alertCaptor.getValue();

        assertEquals(budgetId, alert.getBudgetId());
        assertEquals(consumptionId, alert.getConsumptionId());
        assertEquals(AlertSeverity.WARNING, alert.getSeverity());
        assertEquals("Budget WARNING: 80.00% consumed for period 2026-01-01 -> 2026-01-31", alert.getMessage());
        assertEquals(PERIOD_START, alert.getPeriodStart());
        assertEquals(PERIOD_END, alert.getPeriodEnd());
        assertEquals(NOW, alert.getCreatedAt());
    }

    @Test
    void shouldCreateCriticalAlertWhenCriticalThresholdIsCrossed() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID consumptionId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                "event-critical"
        );

        Budget budget = budget(
                budgetId,
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        BudgetConsumption existingConsumption = consumption(
                consumptionId,
                budgetId,
                new BigDecimal("900.00")
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-critical",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(idempotencyPort.tryAcquire(
                "alert:" + budgetId + ":" + consumptionId + ":CRITICAL",
                IDEMPOTENCY_TTL,
                SCOPE_BUDGET_ALERT
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget));
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, PERIOD_START, PERIOD_END))
                .thenReturn(Optional.of(existingConsumption));
        when(saveBudgetConsumptionPort.save(existingConsumption)).thenReturn(existingConsumption);
        when(saveBudgetAlertPort.save(any(BudgetAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.process(command);

        ArgumentCaptor<BudgetAlert> alertCaptor = ArgumentCaptor.forClass(BudgetAlert.class);

        verify(saveBudgetAlertPort).save(alertCaptor.capture());
        verify(publishBudgetAlertEventPort).publish(alertCaptor.getValue());

        assertEquals(AlertSeverity.CRITICAL, alertCaptor.getValue().getSeverity());
        assertEquals("Budget CRITICAL: 100.00% consumed for period 2026-01-01 -> 2026-01-31", alertCaptor.getValue().getMessage());
    }

    @Test
    void shouldSuppressAlertWhenSeverityDidNotChangeAndPublishOnEveryUpdateIsDisabled() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID consumptionId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("50.00"),
                "BRL",
                TX_DATE,
                "event-suppressed"
        );

        Budget budget = budget(
                budgetId,
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        BudgetConsumption existingConsumption = consumption(
                consumptionId,
                budgetId,
                new BigDecimal("850.00")
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-suppressed",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget));
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, PERIOD_START, PERIOD_END))
                .thenReturn(Optional.of(existingConsumption));
        when(saveBudgetConsumptionPort.save(existingConsumption)).thenReturn(existingConsumption);

        service.process(command);

        verify(saveBudgetConsumptionPort).save(existingConsumption);
        verify(saveBudgetAlertPort, never()).save(any(BudgetAlert.class));
        verify(publishBudgetAlertEventPort, never()).publish(any(BudgetAlert.class));
    }

    @Test
    void shouldPublishAlertWhenSeverityDidNotChangeButPublishOnEveryUpdateIsEnabled() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID consumptionId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("50.00"),
                "BRL",
                TX_DATE,
                "event-publish-every-update"
        );

        Budget budget = budget(
                budgetId,
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        BudgetConsumption existingConsumption = consumption(
                consumptionId,
                budgetId,
                new BigDecimal("850.00")
        );

        when(props.alerts().publishOnEveryUpdate()).thenReturn(true);
        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-publish-every-update",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(idempotencyPort.tryAcquire(
                "alert:" + budgetId + ":" + consumptionId + ":WARNING",
                IDEMPOTENCY_TTL,
                SCOPE_BUDGET_ALERT
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget));
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, PERIOD_START, PERIOD_END))
                .thenReturn(Optional.of(existingConsumption));
        when(saveBudgetConsumptionPort.save(existingConsumption)).thenReturn(existingConsumption);
        when(saveBudgetAlertPort.save(any(BudgetAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.process(command);

        verify(saveBudgetAlertPort).save(any(BudgetAlert.class));
        verify(publishBudgetAlertEventPort).publish(any(BudgetAlert.class));
    }

    @Test
    void shouldNotCreateAlertWhenAlertIdempotencyAlreadyExists() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID consumptionId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                "event-alert-idempotency"
        );

        Budget budget = budget(
                budgetId,
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        BudgetConsumption existingConsumption = consumption(
                consumptionId,
                budgetId,
                new BigDecimal("700.00")
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-alert-idempotency",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(idempotencyPort.tryAcquire(
                "alert:" + budgetId + ":" + consumptionId + ":WARNING",
                IDEMPOTENCY_TTL,
                SCOPE_BUDGET_ALERT
        )).thenReturn(false);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget));
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, PERIOD_START, PERIOD_END))
                .thenReturn(Optional.of(existingConsumption));
        when(saveBudgetConsumptionPort.save(existingConsumption)).thenReturn(existingConsumption);

        service.process(command);

        verify(saveBudgetAlertPort, never()).save(any(BudgetAlert.class));
        verify(publishBudgetAlertEventPort, never()).publish(any(BudgetAlert.class));
    }

    @Test
    void shouldWrapExceptionWhenConsumptionProcessingFails() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();

        RuntimeException cause = new RuntimeException("database error");

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                "event-error"
        );

        Budget budget = budget(
                budgetId,
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-error",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget));
        when(loadBudgetConsumptionPort.findByBudgetAndPeriod(budgetId, PERIOD_START, PERIOD_END))
                .thenReturn(Optional.empty());
        when(saveBudgetConsumptionPort.save(any(BudgetConsumption.class))).thenThrow(cause);

        BudgetProjectionProcessingException exception = assertThrows(
                BudgetProjectionProcessingException.class,
                () -> service.process(command)
        );

        assertSame(cause, exception.getCause());

        verify(saveBudgetAlertPort, never()).save(any(BudgetAlert.class));
        verify(publishBudgetAlertEventPort, never()).publish(any(BudgetAlert.class));
    }

    @Test
    void shouldThrowInvalidCurrencyCodeExceptionWhenCurrencyBecomesInvalidAfterBudgetLookup() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                transactionId,
                userId,
                categoryId,
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                "event-invalid-after-lookup"
        );

        when(command.currency()).thenReturn("BRL", "INVALID");

        Budget budget = budget(
                UUID.randomUUID(),
                userId,
                categoryId,
                BudgetStatus.ACTIVE,
                PERIOD_START,
                PERIOD_END,
                new BigDecimal("1000.00")
        );

        when(idempotencyPort.tryAcquire(
                "kafka:categorized-tx:event:event-invalid-after-lookup",
                IDEMPOTENCY_TTL,
                SCOPE_KAFKA_CATEGORIZED_TX
        )).thenReturn(true);
        when(loadBudgetsPort.findByUserId(userId)).thenReturn(List.of(budget));

        assertThrows(
                InvalidCurrencyCodeException.class,
                () -> service.process(command)
        );

        verify(saveBudgetConsumptionPort, never()).save(any(BudgetConsumption.class));
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenCommandIsNull() {
        assertThrows(
                InvalidFieldException.class,
                () -> service.process(null)
        );
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenRunIdIsNull() {
        ProcessTransactionCommand command = command(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                null
        );

        assertThrows(
                InvalidFieldException.class,
                () -> service.process(command)
        );
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenTransactionIdIsNull() {
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                null
        );

        assertThrows(
                InvalidFieldException.class,
                () -> service.process(command)
        );
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenUserIdIsNull() {
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                null
        );

        assertThrows(
                InvalidFieldException.class,
                () -> service.process(command)
        );
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenCategoryIdIsNull() {
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                new BigDecimal("100.00"),
                "BRL",
                TX_DATE,
                null
        );

        assertThrows(
                InvalidFieldException.class,
                () -> service.process(command)
        );
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenAmountIsNull() {
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "BRL",
                TX_DATE,
                null
        );

        assertThrows(
                InvalidFieldException.class,
                () -> service.process(command)
        );
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenAmountIsNegative() {
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("-1.00"),
                "BRL",
                TX_DATE,
                null
        );

        assertThrows(
                InvalidFieldException.class,
                () -> service.process(command)
        );
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenCurrencyIsNull() {
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                null,
                TX_DATE,
                null
        );

        assertThrows(
                InvalidFieldException.class,
                () -> service.process(command)
        );
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenCurrencyIsBlank() {
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "   ",
                TX_DATE,
                null
        );

        assertThrows(
                InvalidFieldException.class,
                () -> service.process(command)
        );
    }

    @Test
    void shouldThrowInvalidFieldExceptionWhenTransactionDateIsNull() {
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "BRL",
                null,
                null
        );

        assertThrows(
                InvalidFieldException.class,
                () -> service.process(command)
        );
    }

    @Test
    void shouldThrowInvalidCurrencyCodeExceptionWhenCurrencyIsInvalid() {
        ProcessTransactionCommand command = command(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "INVALID",
                TX_DATE,
                null
        );

        assertThrows(
                InvalidCurrencyCodeException.class,
                () -> service.process(command)
        );
    }

    private static ProcessTransactionCommand command(
            UUID runId,
            UUID transactionId,
            UUID userId,
            UUID categoryId,
            BigDecimal amount,
            String currency,
            LocalDate transactionDate,
            String eventId
    ) {
        ProcessTransactionCommand command = mock(ProcessTransactionCommand.class, Answers.RETURNS_DEEP_STUBS);

        when(command.runId()).thenReturn(runId);
        when(command.transactionId()).thenReturn(transactionId);
        when(command.userId()).thenReturn(userId);
        when(command.categoryId()).thenReturn(categoryId);
        when(command.amount()).thenReturn(amount);
        when(command.currency()).thenReturn(currency);
        when(command.transactionDate()).thenReturn(transactionDate);

        if (eventId == null) {
            when(command.metadata()).thenReturn(null);
        } else {
            when(command.metadata().eventId()).thenReturn(eventId);
        }

        return command;
    }

    private static Budget budget(
            UUID budgetId,
            UUID userId,
            UUID categoryId,
            BudgetStatus status,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal limitAmount
    ) {
        return new Budget(
                budgetId,
                new BudgetKey(
                        new UserId(userId),
                        new CategoryId(categoryId),
                        new Period(periodStart, periodEnd)
                ),
                BudgetPeriodType.values()[0],
                new Money(limitAmount, Currency.getInstance("BRL")),
                status,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }

    private static BudgetConsumption consumption(
            UUID consumptionId,
            UUID budgetId,
            BigDecimal consumedAmount
    ) {
        return new BudgetConsumption(
                consumptionId,
                budgetId,
                PERIOD_START,
                PERIOD_END,
                new Money(consumedAmount, Currency.getInstance("BRL")),
                ConsumptionSource.CATEGORIZED_TX,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }
}