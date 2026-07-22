package br.com.ecofy.ms_budgeting.adapters.out.persistence.repository;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetEntity;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<BudgetEntity, UUID> {

    List<BudgetEntity> findByUserId(UUID userId);

    Page<BudgetEntity> findByUserId(UUID userId, Pageable pageable);

    Optional<BudgetEntity> findByNaturalKey(String naturalKey);

    boolean existsByNaturalKey(String naturalKey);

    List<BudgetEntity> findByStatus(BudgetStatus status);
}
