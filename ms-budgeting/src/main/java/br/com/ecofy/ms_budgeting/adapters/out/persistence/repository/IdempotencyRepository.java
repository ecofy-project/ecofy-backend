package br.com.ecofy.ms_budgeting.adapters.out.persistence.repository;


import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {

    boolean existsByKey(String key);

    // Busca pela CHAVE textual (coluna idem_key), não pelo PK numérico.
    Optional<IdempotencyKeyEntity> findByKey(String key);

    // Remove pela CHAVE textual (usado ao re-adquirir uma chave expirada).
    long deleteByKey(String key);

}