package br.com.ecofy.ms_ingestion.core.application.exception;

import java.util.UUID;

// Sinaliza que o conteúdo já foi importado, apontando o job anterior em vez de tratar o replay como falha.
public class ImportAlreadyProcessedException extends IngestionException {

    private final UUID existingJobId;

    public ImportAlreadyProcessedException(UUID existingJobId, String fileHash) {
        super(
                IngestionErrorCode.IMPORT_ALREADY_PROCESSED,
                "This file has already been imported",
                "existingJobId=" + existingJobId + ", fileHash=" + fileHash
        );
        this.existingJobId = existingJobId;
    }

    public UUID getExistingJobId() {
        return existingJobId;
    }
}
