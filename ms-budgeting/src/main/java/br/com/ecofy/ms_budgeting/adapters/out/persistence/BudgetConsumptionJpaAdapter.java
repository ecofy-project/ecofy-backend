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
public class BudgetConsumptionJpaAdapter implements SaveBudgetConsumptionPort, LoadBudgetConsumptionPort {

    private final BudgetConsumptionRepository repository;

    // Persiste um BudgetConsumption no banco via JPA e retorna o domínio reidratado a partir da entidade salva.
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

    // Busca o consumo de um budget para um período específico (start/end) e retorna Optional se não existir.
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

    // Busca o consumo mais recente (por updatedAt) de um budget específico.
    @Transactional(readOnly = true)
    public Optional<BudgetConsumption> findLatestByBudgetId(UUID budgetId) {
        Objects.requireNonNull(budgetId, "budgetId must not be null");

        return repository.findTopByBudgetIdOrderByUpdatedAtDesc(budgetId)
                .map(BudgetConsumptionMapper::toDomain);
    }
}
