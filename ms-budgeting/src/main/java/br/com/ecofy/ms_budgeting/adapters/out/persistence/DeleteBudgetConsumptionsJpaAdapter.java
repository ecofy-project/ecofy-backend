package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetConsumptionJpaRepository;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteBudgetConsumptionsOlderThanPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteBudgetConsumptionsJpaAdapter implements DeleteBudgetConsumptionsOlderThanPort {

    private final BudgetConsumptionJpaRepository repository;

    // Remove consumos com referenceDate <= cutoffDateInclusive e retorna a quantidade deletada.
    @Override
    @Transactional
    public long deleteConsumptionsOlderThan(LocalDate cutoffDateInclusive) {
        Objects.requireNonNull(cutoffDateInclusive, "cutoffDateInclusive must not be null");

        long deleted = repository.deleteByReferenceDateLessThanEqual(cutoffDateInclusive);

        log.info(
                "[DeleteBudgetConsumptionsJpaAdapter] - [deleteConsumptionsOlderThan] -> cutoffDateInclusive={} deleted={}",
                cutoffDateInclusive, deleted
        );

        return deleted;
    }

}
