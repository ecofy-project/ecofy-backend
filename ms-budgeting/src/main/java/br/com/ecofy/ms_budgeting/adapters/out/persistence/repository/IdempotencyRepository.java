package br.com.ecofy.ms_budgeting.adapters.out.persistence.repository;


import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {

    boolean existsByKey(String key);

    Optional<IdempotencyKeyEntity> findByKey(String key);

    long deleteByKey(String key);

}