package br.com.ecofy.ms_budgeting.adapters.out.persistence.repository;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetConsumptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

// Correção Dia 6 (item #3): a entidade usa @Id UUID, mas o repository declarava Long,
// causando modelagem JPA incorreta. Tipo do ID padronizado para UUID.
public interface BudgetConsumptionJpaRepository extends JpaRepository<BudgetConsumptionEntity, UUID> {

    long deleteByReferenceDateLessThanEqual(LocalDate cutoff);

}