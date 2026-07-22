package br.com.ecofy.ms_budgeting.core.port.out;

import br.com.ecofy.ms_budgeting.core.domain.Budget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadBudgetsPort {

    Optional<Budget> findById(UUID id);

    List<Budget> findByUserId(UUID userId);

    PageResult<Budget> findByUserId(UUID ownerId, int page, int size, String sortField, boolean ascending);

    boolean existsByNaturalKey(String naturalKey);

    List<Budget> findAllActive();
}
