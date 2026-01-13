package br.com.ecofy.ms_users.adapters.out.persistence.repository;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {
    Optional<IdempotencyKeyEntity> findByOperationAndKey(String operation, String key);
    long deleteByExpiresAtBefore(Instant cutoff);
}