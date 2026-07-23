package br.com.ecofy.ms_ingestion.adapters.out.persistence.repository;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImportFileRepository extends JpaRepository<ImportFileEntity, UUID> {

    Optional<ImportFileEntity> findByUserIdAndFileHash(UUID userId, String fileHash);

    Optional<ImportFileEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
