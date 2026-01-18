package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.core.application.result.BudgetConsumptionResult;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetOverviewResult;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetResult;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.exception.BudgetNotFoundException;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetOverviewUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.ListBudgetsUseCase;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class BudgetQueryService implements ListBudgetsUseCase, GetBudgetUseCase, GetBudgetOverviewUseCase {

    private final LoadBudgetsPort loadBudgetsPort;

    // Injeta o port de leitura de budgets para atender casos de uso de consulta.
    public BudgetQueryService(LoadBudgetsPort loadBudgetsPort) {
        this.loadBudgetsPort = Objects.requireNonNull(loadBudgetsPort, "loadBudgetsPort must not be null");
    }

    // Lista budgets de um usuário e converte para BudgetResult.
    @Override
    public List<BudgetResult> listByUser(UUID userId) {
        requireNonNull(userId, "userId");

        var budgets = loadBudgetsPort.findByUserId(userId);

        log.debug("[BudgetQueryService] - [listByUser] -> userId={} budgets={}", userId, budgets.size());

        return budgets.stream()
                .map(BudgetQueryService::toResult)
                .toList();
    }

    // Busca um budget por id e lança exceção se não existir.
    @Override
    public BudgetResult get(UUID budgetId) {
        requireNonNull(budgetId, "budgetId");

        return loadBudgetsPort.findById(budgetId)
                .map(BudgetQueryService::toResult)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));
    }

    // Retorna visão geral do usuário (budgets + consumos/alertas) com consumos stub até existir port agregado.
    @Override
    public BudgetOverviewResult overview(UUID userId) {
        requireNonNull(userId, "userId");

        var budgets = loadBudgetsPort.findByUserId(userId);

        var consumptions = budgets.stream()
                .map(b -> toConsumptionStub(b))
                .toList();

        log.debug("[BudgetQueryService] - [overview] -> userId={} budgets={}", userId, budgets.size());

        return new BudgetOverviewResult(userId, consumptions, List.of());
    }

    // Cria um consumo “placeholder” zerado para o budget até haver leitura de consumo agregado.
    private static BudgetConsumptionResult toConsumptionStub(Budget b) {
        return new BudgetConsumptionResult(
                b.getId(),
                BigDecimal.ZERO,
                b.getLimit().amount(),
                BigDecimal.ZERO
        );
    }

    // Converte Budget (domínio) para BudgetResult (DTO de aplicação).
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
                b.getUpdatedAt()
        );
    }

    // Valida referência não nula com mensagem de erro padronizada.
    private static <T> T requireNonNull(T v, String field) {
        return Objects.requireNonNull(v, field + " must not be null");
    }

}
