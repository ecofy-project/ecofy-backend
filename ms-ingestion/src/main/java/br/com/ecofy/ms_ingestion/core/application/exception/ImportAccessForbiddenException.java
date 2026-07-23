package br.com.ecofy.ms_ingestion.core.application.exception;

import java.util.UUID;

// Sinaliza acesso a job de outro usuário, sem confirmar a existência do recurso na resposta.
public class ImportAccessForbiddenException extends IngestionException {

    public ImportAccessForbiddenException(UUID jobId) {
        super(
                IngestionErrorCode.IMPORT_ACCESS_FORBIDDEN,
                "Access to this import job is forbidden",
                "jobId=" + jobId
        );
    }
}
