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

@Slf4j
@Service
public class BudgetQueryService implements ListBudgetsUseCase, GetBudgetUseCase, GetBudgetOverviewUseCase {

    private final LoadBudgetsPort loadBudgetsPort;
    private final LoadBudgetConsumptionPort loadBudgetConsumptionPort;

    // Injeta os ports de leitura de budgets e de consumo garantindo dependências não nulas.
    public BudgetQueryService(LoadBudgetsPort loadBudgetsPort,
                              LoadBudgetConsumptionPort loadBudgetConsumptionPort) {
        this.loadBudgetsPort = Objects.requireNonNull(loadBudgetsPort, "loadBudgetsPort must not be null");
        this.loadBudgetConsumptionPort = Objects.requireNonNull(loadBudgetConsumptionPort, "loadBudgetConsumptionPort must not be null");
    }

    // Lista budgets de um usuário, validando o userId, consultando o repositório e mapeando entidades para DTOs de resposta.
    @Override
    public List<BudgetResult> listByUser(UUID userId) {
        requireNonNull(userId, "userId");

        var budgets = loadBudgetsPort.findByUserId(userId);

        log.debug("[BudgetQueryService] - [listByUser] -> userId={} budgets={}", userId, budgets.size());

        return budgets.stream()
                .map(BudgetQueryService::toResult)
                .toList();
    }

    // Busca um budget por id, validando entrada, convertendo para DTO e lançando exceção de não encontrado quando necessário.
    @Override
    public BudgetResult get(UUID budgetId) {
        requireNonNull(budgetId, "budgetId");

        return loadBudgetsPort.findById(budgetId)
                .map(BudgetQueryService::toResult)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));
    }

    // Retorna um overview HONESTO do usuário: carrega o consumo real de cada budget no seu período
    // (antes retornava sempre zeros, o que era enganoso).
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

    // Monta o resumo de consumo de um budget com valores REAIS (consumido + %), carregando o consumo do período.
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

    // Converte a entidade Budget em BudgetResult, extraindo chave, período, limite, moeda, status e timestamps.
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

    // Valida campo obrigatório (não nulo) e lança InvalidFieldException com o nome do campo para padronizar erros de entrada.
    private static <T> T requireNonNull(T v, String field) {
        if (v == null) throw InvalidFieldException.required(field);
        return v;
    }

}
