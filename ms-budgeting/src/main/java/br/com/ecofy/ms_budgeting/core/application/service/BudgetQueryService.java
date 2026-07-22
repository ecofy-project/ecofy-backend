package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.core.application.exception.InvalidFieldException;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetConsumptionResult;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetOverviewResult;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetResult;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.BudgetConsumption;
import br.com.ecofy.ms_budgeting.core.domain.exception.BudgetNotFoundException;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetOverviewUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.ListBudgetsUseCase;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetConsumptionPort;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Centraliza as consultas e projeções de leitura dos orçamentos.
@Slf4j
@Service
public class BudgetQueryService implements ListBudgetsUseCase, GetBudgetUseCase, GetBudgetOverviewUseCase {

    private final LoadBudgetsPort loadBudgetsPort;
    private final LoadBudgetConsumptionPort loadBudgetConsumptionPort;

    public BudgetQueryService(LoadBudgetsPort loadBudgetsPort,
                              LoadBudgetConsumptionPort loadBudgetConsumptionPort) {
        this.loadBudgetsPort = Objects.requireNonNull(loadBudgetsPort, "loadBudgetsPort must not be null");
        this.loadBudgetConsumptionPort = Objects.requireNonNull(loadBudgetConsumptionPort, "loadBudgetConsumptionPort must not be null");
    }

    // Consulta os orçamentos do usuário e converte os resultados para a camada de aplicação.
    @Override
    public List<BudgetResult> listByUser(UUID userId) {
        requireNonNull(userId, "userId");

        var budgets = loadBudgetsPort.findByUserId(userId);

        log.debug("[BudgetQueryService] - [listByUser] -> userId={} budgets={}", userId, budgets.size());

        return budgets.stream()
                .map(BudgetQueryService::toResult)
                .toList();
    }

    // Consulta o histórico paginado de orçamentos pertencentes ao usuário.
    @Override
    public br.com.ecofy.ms_budgeting.core.port.out.PageResult<BudgetResult> list(ListBudgetsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        requireNonNull(query.ownerId(), "ownerId");

        var page = loadBudgetsPort.findByUserId(
                query.ownerId(), query.page(), query.size(), query.sortField(), query.ascending());

        return new br.com.ecofy.ms_budgeting.core.port.out.PageResult<>(
                page.content().stream().map(BudgetQueryService::toResult).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }

    @Override
    public BudgetResult get(UUID budgetId) {
        requireNonNull(budgetId, "budgetId");

        return loadBudgetsPort.findById(budgetId)
                .map(BudgetQueryService::toResult)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));
    }

    // Consolida os orçamentos com seus consumos reais por período.
    @Override
    public BudgetOverviewResult overview(UUID userId) {
        requireNonNull(userId, "userId");

        var budgets = loadBudgetsPort.findByUserId(userId);

        var consumptions = budgets.stream()
                .map(this::toConsumption)
                .toList();

        log.debug("[BudgetQueryService] - [overview] -> userId={} budgets={}", userId, budgets.size());

        return new BudgetOverviewResult(userId, consumptions, List.of());
    }

    // Calcula o consumo e o percentual utilizado no período do orçamento.
    private BudgetConsumptionResult toConsumption(Budget b) {
        BigDecimal limit = b.getLimit().amount();
        var period = b.getKey().period();

        BigDecimal consumed = loadBudgetConsumptionPort
                .findByBudgetAndPeriod(b.getId(), period.start(), period.end())
                .map(BudgetConsumption::getConsumed)
                .map(m -> m.amount())
                .orElse(BigDecimal.ZERO);

        BigDecimal pct = (limit == null || limit.signum() <= 0)
                ? BigDecimal.ZERO
                : consumed.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP);

        return new BudgetConsumptionResult(b.getId(), consumed, limit, pct);
    }

    private static BudgetResult toResult(Budget b) {
        return new BudgetResult(
                b.getId(),
                b.getKey().userId().value(),
                b.getKey().categoryId().value(),
                b.getPeriodType(),
                b.getKey().period().start(),
                b.getKey().period().end(),
                b.getLimit().amount(),
                b.getLimit().currency().getCurrencyCode(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getUpdatedAt(),
                b.getVersion()
        );
    }

    private static <T> T requireNonNull(T v, String field) {
        if (v == null) throw InvalidFieldException.required(field);
        return v;
    }
}
