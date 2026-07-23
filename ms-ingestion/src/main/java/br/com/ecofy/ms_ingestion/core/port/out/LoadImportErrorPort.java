package br.com.ecofy.ms_ingestion.core.port.out;

import br.com.ecofy.ms_ingestion.core.domain.ImportError;

import java.util.List;
import java.util.UUID;

public interface LoadImportErrorPort {

    List<ImportError> findByJobId(UUID jobId, int limit);
}
