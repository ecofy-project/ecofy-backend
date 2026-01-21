package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.core.application.exception.ImportJobNotFoundException;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.port.in.GetImportJobStatusUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.LoadImportJobPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class ImportJobQueryService implements GetImportJobStatusUseCase {

    private final LoadImportJobPort loadImportJobPort;

    public ImportJobQueryService(LoadImportJobPort loadImportJobPort) {
        this.loadImportJobPort = Objects.requireNonNull(loadImportJobPort, "loadImportJobPort must not be null");
    }

    // Consulta e retorna o status do ImportJob (e erros associados quando aplicável) a partir do jobId.
    @Override
    public ImportJobStatusView getById(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        log.debug("[ImportJobQueryService] - [getById] -> Buscando status de job jobId={}", jobId);

        ImportJob job = loadImportJobPort.loadById(jobId)
                .orElseThrow(() -> new ImportJobNotFoundException(jobId));

        List<ImportError> errors = Collections.emptyList();
        return new ImportJobStatusView(job, errors);
    }

}
