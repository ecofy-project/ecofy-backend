package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportAccessForbiddenException;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportJobNotFoundException;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.port.in.GetImportJobStatusUseCase;
import br.com.ecofy.ms_ingestion.core.port.in.ListImportJobsUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.LoadImportErrorPort;
import br.com.ecofy.ms_ingestion.core.port.out.LoadImportJobPort;
import br.com.ecofy.ms_ingestion.core.port.out.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Executa as consultas de importação sempre aplicando o dono autenticado ao filtro.
@Slf4j
@Service
public class ImportJobQueryService implements GetImportJobStatusUseCase, ListImportJobsUseCase {

    private final LoadImportJobPort loadImportJobPort;
    private final LoadImportErrorPort loadImportErrorPort;
    private final IngestionProperties properties;

    public ImportJobQueryService(LoadImportJobPort loadImportJobPort,
                                 LoadImportErrorPort loadImportErrorPort,
                                 IngestionProperties properties) {
        this.loadImportJobPort = Objects.requireNonNull(loadImportJobPort, "loadImportJobPort must not be null");
        this.loadImportErrorPort = Objects.requireNonNull(loadImportErrorPort, "loadImportErrorPort must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public ImportJobStatusView getById(UUID jobId, UUID ownerId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        log.debug("[ImportJobQueryService] - [getById] -> Buscando status de job jobId={}", jobId);

        ImportJob job = loadImportJobPort.loadById(jobId)
                .orElseThrow(() -> new ImportJobNotFoundException(jobId));

        if (!job.userId().equals(ownerId)) {
            // Existe, mas não é seu. O handler responde 403 sem confirmar a existência.
            throw new ImportAccessForbiddenException(jobId);
        }

        List<ImportError> errors = loadImportErrorPort.findByJobId(
                jobId, properties.getUpload().getMaxRecordedErrors());

        return new ImportJobStatusView(job, errors);
    }

    @Override
    public PageResult<ImportJob> list(ListImportJobsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(query.ownerId(), "ownerId must not be null");

        log.debug("[ImportJobQueryService] - [list] -> Listando jobs page={} size={} status={}",
                query.page(), query.size(), query.status());

        return loadImportJobPort.findByOwner(
                query.ownerId(),
                query.status(),
                query.page(),
                query.size(),
                query.sortField(),
                query.ascending());
    }
}
