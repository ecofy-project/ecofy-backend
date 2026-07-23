package br.com.ecofy.ms_ingestion.core.port.out;

import br.com.ecofy.ms_ingestion.core.domain.ImportFile;

import java.util.Optional;
import java.util.UUID;

public interface SaveImportFilePort {

    ImportFile save(ImportFile file);

    ImportFile getById(UUID id);

    Optional<ImportFile> findByOwnerAndHash(UUID ownerId, String fileHash);

    Optional<ImportFile> findByOwnerAndIdempotencyKey(UUID ownerId, String idempotencyKey);
}
