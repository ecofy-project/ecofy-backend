package br.com.ecofy.ms_ingestion.core.port.in;

import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;

import java.util.List;
import java.util.UUID;

public interface GetImportJobStatusUseCase {

    record ImportJobStatusView(ImportJob job, List<ImportError> errors) {
    }

    ImportJobStatusView getById(UUID jobId, UUID ownerId);
}
