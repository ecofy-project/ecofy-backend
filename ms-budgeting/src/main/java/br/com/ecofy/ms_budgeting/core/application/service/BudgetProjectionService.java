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
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.enums.ConsumptionSource;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Period;
import br.com.ecofy.ms_budgeting.core.port.in.ProcessTransactionForBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.out.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class BudgetProjectionService implements ProcessTransactionForBudgetUseCase {

    private static final String SCOPE_KAFKA_CATEGORIZED_TX = "kafka:categorizedTx";

    private final LoadBudgetsPort loadBudgetsPort;
    private final LoadBudgetConsumptionPort loadBudgetConsumptionPort;
    private final SaveBudgetConsumptionPort saveBudgetConsumptionPort;
    private final SaveBudgetAlertPort saveBudgetAlertPort;
    private final PublishBudgetAlertEventPort publishBudgetAlertEventPort;
    private final IdempotencyPort idempotencyPort;
    private final BudgetingProperties props;
    private final Clock clock;

    // Inicializa o serviço injetando dependências (ports), propriedades de negócio e clock para facilitar testes/determinismo.
    public BudgetProjectionService(
            LoadBudgetsPort loadBudgetsPort,
            LoadBudgetConsumptionPort loadBudgetConsumptionPort,
            SaveBudgetConsumptionPort saveBudgetConsumptionPort,
            SaveBudgetAlertPort saveBudgetAlertPort,
            PublishBudgetAlertEventPort publishBudgetAlertEventPort,
            IdempotencyPort idempotencyPort,
            BudgetingProperties props,
            Clock clock
    ) {
        this.loadBudgetsPort = Objects.requireNonNull(loadBudgetsPort, "loadBudgetsPort must not be null");
        this.loadBudgetConsumptionPort = Objects.requireNonNull(loadBudgetConsumptionPort, "loadBudgetConsumptionPort must not be null");
        this.saveBudgetConsumptionPort = Objects.requireNonNull(saveBudgetConsumptionPort, "saveBudgetConsumptionPort must not be null");
        this.saveBudgetAlertPort = Objects.requireNonNull(saveBudgetAlertPort, "saveBudgetAlertPort must not be null");
        this.publishBudgetAlertEventPort = Objects.requireNonNull(publishBudgetAlertEventPort, "publishBudgetAlertEventPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Processa uma transação categorizada aplicando idempotência, buscando budgets elegíveis e atualizando consumo/alertas dentro de uma transação.
    @Override
    @Transactional
    public void process(ProcessTransactionCommand cmd) {
        validate(cmd);

        String eventId = cmd.metadata() != null ? cmd.metadata().eventId() : null;
        String idemKey = buildIdempotencyKey(eventId, String.valueOf(cmd.transactionId()));

        boolean acquired = idempotencyPort.tryAcquire(idemKey, props.idempotency().ttl(), SCOPE_KAFKA_CATEGORIZED_TX);
        if (!acquired) {
            log.warn("[BudgetProjectionService] - [process] -> Idempotency hit key={} txId={}", idemKey, cmd.transactionId());
            return;
        }

        List<Budget> budgets = loadBudgetsPort.findByUserId(UUID.fromString(String.valueOf(cmd.userId())));
        if (budgets.isEmpty()) {
            log.debug("[BudgetProjectionService] - [process] -> No budgets for userId={}", cmd.userId());
            return;
        }

        final Currency currency;
        try {
            currency = Currency.getInstance(cmd.currency().trim().toUpperCase());
        } catch (Exception ex) {
            throw new InvalidCurrencyCodeException(cmd.currency(), ex);
        }

        Money delta = new Money(cmd.amount(), currency);
        LocalDate txDate = cmd.transactionDate();

        try {
            for (Budget b : budgets) {
                if (b.getStatus() != BudgetStatus.ACTIVE) continue;
                if (!b.getKey().categoryId().value().equals(cmd.categoryId())) continue;

                Period budgetPeriod = b.getKey().period();
                if (!budgetPeriod.contains(txDate)) continue;

                upsertConsumptionAndMaybeAlert(b, delta, txDate, eventId);
            }
        } catch (Exception ex) {
            throw new BudgetProjectionProcessingException(
                    "Failed to process categorized transaction for budgets. txId=" + cmd.transactionId()
                            + " userId=" + cmd.userId()
                            + " categoryId=" + cmd.categoryId()
                            + " eventId=" + eventId,
                    ex
            );
        }
    }

    // Atualiza (ou cria) o consumo do budget no período e publica alerta quando cruza thresholds ou quando configurado para publicar em toda atualização.
    private void upsertConsumptionAndMaybeAlert(Budget budget, Money delta, LocalDate txDate, String eventId) {
        Instant now = Instant.now(clock);

        Period period = budget.getKey().period();
        UUID budgetId = budget.getId();
        Money limit = budget.getLimit();

        BudgetConsumption consumption = loadBudgetConsumptionPort
                .findByBudgetAndPeriod(budgetId, period.start(), period.end())
                .orElseGet(() -> new BudgetConsumption(
                        UUID.randomUUID(),
                        budgetId,
                        period.start(),
                        period.end(),
                        Money.zero(limit.currency()),
                        ConsumptionSource.CATEGORIZED_TX,
                        now,
                        now
                ));

        BigDecimal before = consumption.getConsumed().amount();
        consumption.add(delta, now);
        BudgetConsumption saved = saveBudgetConsumptionPort.save(consumption);

        BigDecimal after = saved.getConsumed().amount();
        BigDecimal pctBefore = toPct(before, limit.amount());
        BigDecimal pctAfter = toPct(after, limit.amount());

        AlertSeverity severityAfter = resolveSeverity(pctAfter);
        if (severityAfter == null) {
            if (props.alerts().publishOnEveryUpdate()) {
                log.debug("[BudgetProjectionService] - [consumption] -> budgetId={} pctAfter={} amountAfter={}",
                        budgetId, pctAfter, after);
            }
            return;
        }

        AlertSeverity severityBefore = resolveSeverity(pctBefore);
        boolean crossed = severityBefore != severityAfter;

        if (!crossed && !props.alerts().publishOnEveryUpdate()) {
            log.debug("[BudgetProjectionService] - [alert] -> suppressed (already in same severity) budgetId={} severity={} pctAfter={}",
                    budgetId, severityAfter, pctAfter);
            return;
        }

        String alertIdemKey = "alert:" + budgetId + ":" + saved.getId() + ":" + severityAfter;
        if (!idempotencyPort.tryAcquire(alertIdemKey, props.idempotency().ttl(), "budget:alert")) {
            log.debug("[BudgetProjectionService] - [alert] -> idempotency hit budgetId={} severity={}", budgetId, severityAfter);
            return;
        }

        String msg = buildAlertMessage(severityAfter, pctAfter, saved.getPeriodStart(), saved.getPeriodEnd());

        BudgetAlert alert = new BudgetAlert(
                UUID.randomUUID(),
                budgetId,
                saved.getId(),
                severityAfter,
                msg,
                saved.getPeriodStart(),
                saved.getPeriodEnd(),
                now
        );

        BudgetAlert persisted = saveBudgetAlertPort.save(alert);
        publishBudgetAlertEventPort.publish(persisted);

        log.info("[BudgetProjectionService] - [alert] -> budgetId={} severity={} pctAfter={} txDate={} eventId={}",
                budgetId, severityAfter, pctAfter, txDate, eventId);
    }

    // Converte valores consumido/limite em porcentagem (0–100+) com escala 2 e rounding HALF_UP, tratando nulos e limite inválido.
    private static BigDecimal toPct(BigDecimal consumed, BigDecimal limit) {
        if (limit == null || limit.signum() <= 0) return BigDecimal.ZERO;
        if (consumed == null) return BigDecimal.ZERO;

        return consumed
                .multiply(BigDecimal.valueOf(100))
                .divide(limit, 2, RoundingMode.HALF_UP);
    }

    // Determina a severidade do alerta com base nos thresholds configurados (CRITICAL >= critical, WARNING >= warning, senão nenhum alerta).
    private AlertSeverity resolveSeverity(BigDecimal pct) {
        if (pct == null) return null;

        if (pct.compareTo(props.alerts().criticalThresholdPct()) >= 0) return AlertSeverity.CRITICAL;
        if (pct.compareTo(props.alerts().warningThresholdPct()) >= 0) return AlertSeverity.WARNING;
        return null;
    }

    // Monta a mensagem textual do alerta incluindo severidade, percentual consumido e intervalo do período do budget.
    private static String buildAlertMessage(AlertSeverity severity, BigDecimal pct, LocalDate start, LocalDate end) {
        return "Budget " + severity + ": " + pct + "% consumed for period " + start + " -> " + end;
    }

    // Gera a chave de idempotência priorizando eventId (quando presente) e caindo para transactionId quando não há eventId.
    private static String buildIdempotencyKey(String eventId, String transactionId) {
        if (eventId != null && !eventId.isBlank()) {
            return "kafka:categorized-tx:event:" + eventId.trim();
        }
        return "kafka:categorized-tx:tx:" + transactionId;
    }

    // Valida campos obrigatórios e regras básicas (amount >= 0, currency não vazia e ISO-4217 válida, transactionDate presente) antes do processamento.
    private static void validate(ProcessTransactionCommand cmd) {
        if (cmd == null) throw InvalidFieldException.required("cmd");
        if (cmd.runId() == null) throw InvalidFieldException.required("runId");
        if (cmd.transactionId() == null) throw InvalidFieldException.required("transactionId");
        if (cmd.userId() == null) throw InvalidFieldException.required("userId");
        if (cmd.categoryId() == null) throw InvalidFieldException.required("categoryId");

        if (cmd.amount() == null) throw InvalidFieldException.required("amount");
        if (cmd.amount().signum() < 0) throw InvalidFieldException.invalid("amount", "must be >= 0");

        if (cmd.currency() == null || cmd.currency().trim().isEmpty()) throw InvalidFieldException.notBlank("currency");

        if (cmd.transactionDate() == null) throw InvalidFieldException.required("transactionDate");

        // valida ISO-4217
        try {
            Currency.getInstance(cmd.currency().trim().toUpperCase());
        } catch (Exception ex) {
            throw new InvalidCurrencyCodeException(cmd.currency(), ex);
        }
    }

}
