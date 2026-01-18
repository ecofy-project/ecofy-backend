package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.core.application.command.RecalculateBudgetsCommand;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Period;
import br.com.ecofy.ms_budgeting.core.port.in.RecalculateBudgetsUseCase;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetConsumptionPort;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetRecalculationService implements RecalculateBudgetsUseCase {

    private final LoadBudgetsPort loadBudgetsPort;
    private final LoadBudgetConsumptionPort loadBudgetConsumptionPort;
    private final Clock clock;

    // Recalcula métricas de consumo/percentual dos budgets ativos para uma data de referência.
    @Override
    @Transactional(readOnly = true)
    public void recalculate(RecalculateBudgetsCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");

        LocalDate refDate = cmd.referenceDate() != null ? cmd.referenceDate() : LocalDate.now(clock);
        Instant startedAt = Instant.now(clock);

        log.info("[BudgetRecalculationService] - [recalculate] -> START runId={} referenceDate={} startedAt={}",
                cmd.runId(), refDate, startedAt);

        var budgets = loadBudgetsPort.findAllActive();
        log.info("[BudgetRecalculationService] - [recalculate] -> activeBudgets={}", budgets.size());

        int evaluated = 0;

        for (var b : budgets) {
            Period period = b.getKey().period();

            // se você quer recalcular apenas budgets “vigentes” na data de referência
            if (!period.contains(refDate)) continue;
            evaluated++;

            var consumptionOpt = loadBudgetConsumptionPort.findByBudgetAndPeriod(
                    b.getId(),
                    period.start(),
                    period.end()
            );

            BigDecimal consumed = consumptionOpt
                    .map(c -> c.getConsumed().amount())
                    .orElse(BigDecimal.ZERO);

            BigDecimal limit = b.getLimit().amount();
            BigDecimal pct = pct(consumed, limit);

            log.debug("[BudgetRecalculationService] - [recalculate] -> budgetId={} userId={} categoryId={} period={}..{} consumed={} limit={} pct={}",
                    b.getId(),
                    b.getKey().userId().value(),
                    b.getKey().categoryId().value(),
                    period.start(),
                    period.end(),
                    consumed,
                    limit,
                    pct
            );
        }

        log.info("[BudgetRecalculationService] - [recalculate] -> DONE runId={} referenceDate={} evaluatedBudgets={}",
                cmd.runId(), refDate, evaluated);
    }

    // Calcula o percentual consumido do budget, protegendo contra limite inválido e valores nulos.
    private static BigDecimal pct(BigDecimal consumed, BigDecimal limit) {
        if (limit == null || limit.signum() <= 0) return BigDecimal.ZERO;
        if (consumed == null) return BigDecimal.ZERO;

        return consumed
                .multiply(BigDecimal.valueOf(100))
                .divide(limit, 2, RoundingMode.HALF_UP);
    }

}
