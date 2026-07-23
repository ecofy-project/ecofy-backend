package br.com.ecofy.ms_categorization.adapters.out.persistence.repository;

import br.com.ecofy.ms_categorization.adapters.out.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    @Modifying
    @Query(value = """
            INSERT INTO cat_idempotency_keys (idempotency_key, created_at, expires_at)
            VALUES (:key, :createdAt, :expiresAt)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int tryInsert(@Param("key") String key,
                  @Param("createdAt") Instant createdAt,
                  @Param("expiresAt") Instant expiresAt);
}
