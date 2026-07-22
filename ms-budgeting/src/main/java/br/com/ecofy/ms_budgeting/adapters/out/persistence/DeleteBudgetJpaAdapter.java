package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetJpaRepository;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteBudgetPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
// Centraliza a exclusão e a verificação de existência de orçamentos.
public class DeleteBudgetJpaAdapter implements DeleteBudgetPort {

    private final BudgetJpaRepository repo;

    public DeleteBudgetJpaAdapter(BudgetJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
    }

    // Remove o orçamento quando existente e preserva a idempotência da operação.
    @Override
    @Transactional
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        if (!repo.existsById(id)) {
            log.debug("[DeleteBudgetJpaAdapter] - [deleteById] -> NOT_FOUND id={}", id);
            return;
        }

        repo.deleteById(id);
        log.debug("[DeleteBudgetJpaAdapter] - [deleteById] -> DELETED id={}", id);
    }

    @Override
    public boolean existsById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        boolean exists = repo.existsById(id);

        if (exists) {
            log.debug("[DeleteBudgetJpaAdapter] - [existsById] -> EXISTS id={}", id);
        } else {
            log.debug("[DeleteBudgetJpaAdapter] - [existsById] -> NOT_FOUND id={}", id);
        }

        return exists;
    }
}
