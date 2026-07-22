package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetJpaRepository;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteArchivedBudgetsOlderThanPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
// Centraliza a remoção de orçamentos arquivados.
public class DeleteArchivedBudgetsJpaAdapter implements DeleteArchivedBudgetsOlderThanPort {

    private final BudgetJpaRepository repository;

    // Remove os orçamentos arquivados até a data limite e retorna a quantidade excluída.
    @Override
    @Transactional
    public long deleteArchivedBudgetsOlderThan(LocalDate cutoffDateInclusive) {
        Objects.requireNonNull(cutoffDateInclusive, "cutoffDateInclusive must not be null");

        long deleted = repository.deleteByStatusAndArchivedAtLessThanEqual("ARCHIVED", cutoffDateInclusive);

        log.info(
                "[DeleteArchivedBudgetsJpaAdapter] - [deleteArchivedBudgetsOlderThan] -> cutoffDateInclusive={} deleted={}",
                cutoffDateInclusive, deleted
        );

        return deleted;
    }
}
