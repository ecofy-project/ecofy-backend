package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper.BudgetConsumptionMapper;
import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetConsumptionRepository;
import br.com.ecofy.ms_budgeting.core.domain.BudgetConsumption;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetConsumptionPort;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetConsumptionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
// Centraliza a persistência e a consulta dos consumos de orçamento.
public class BudgetConsumptionJpaAdapter implements SaveBudgetConsumptionPort, LoadBudgetConsumptionPort {

    private final BudgetConsumptionRepository repository;

    // Persiste o consumo e retorna o domínio reconstituído.
    @Override
    @Transactional
    public BudgetConsumption save(BudgetConsumption consumption) {
        Objects.requireNonNull(consumption, "consumption must not be null");

        log.debug(
                "[BudgetConsumptionJpaAdapter] - [save] -> budgetId={} periodStart={} periodEnd={} source={}",
                consumption.getBudgetId(), consumption.getPeriodStart(), consumption.getPeriodEnd(), consumption.getSource()
        );

        var saved = repository.save(BudgetConsumptionMapper.toEntity(consumption));
        return BudgetConsumptionMapper.toDomain(saved);
    }

    // Busca o consumo associado ao orçamento no período informado.
    @Override
    @Transactional(readOnly = true)
    public Optional<BudgetConsumption> findByBudgetAndPeriod(UUID budgetId, LocalDate start, LocalDate end) {
        Objects.requireNonNull(budgetId, "budgetId must not be null");
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must be <= end");
        }

        log.debug(
                "[BudgetConsumptionJpaAdapter] - [findByBudgetAndPeriod] -> budgetId={} start={} end={}",
                budgetId, start, end
        );

        return repository.findByBudgetIdAndPeriodStartAndPeriodEnd(budgetId, start, end)
                .map(BudgetConsumptionMapper::toDomain);
    }

    // Busca o consumo atualizado mais recentemente para o orçamento.
    @Transactional(readOnly = true)
    public Optional<BudgetConsumption> findLatestByBudgetId(UUID budgetId) {
        Objects.requireNonNull(budgetId, "budgetId must not be null");

        return repository.findTopByBudgetIdOrderByUpdatedAtDesc(budgetId)
                .map(BudgetConsumptionMapper::toDomain);
    }
}
