package br.com.ecofy.ms_ingestion.adapters.out.persistence;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportFileEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportJobEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportFileRepository;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportJobRepository;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import br.com.ecofy.ms_ingestion.core.port.out.LoadImportJobPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportJobPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class ImportJobJpaAdapter implements SaveImportJobPort, LoadImportJobPort {

    private final ImportJobRepository importJobRepository;
    private final ImportFileRepository importFileRepository;

    public ImportJobJpaAdapter(ImportJobRepository importJobRepository,
                               ImportFileRepository importFileRepository) {

        this.importJobRepository = Objects.requireNonNull(importJobRepository, "importJobRepository must not be null");
        this.importFileRepository = Objects.requireNonNull(importFileRepository, "importFileRepository must not be null");
    }

    // Persiste um ImportJob garantindo que o ImportFile referenciado exista e retornando o domínio persistido.
    @Override
    @Transactional
    public ImportJob save(ImportJob job) {
        Objects.requireNonNull(job, "job must not be null");

        ImportFileEntity fileEntity = importFileRepository.findById(job.importFileId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "ImportFile not found: " + job.importFileId()
                ));

        ImportJobEntity entity = PersistenceMapper.toEntity(job, fileEntity);
        ImportJobEntity saved = importJobRepository.save(entity);

        return PersistenceMapper.toDomain(saved);
    }

    // Carrega um ImportJob pelo id, retornando Optional para representar ausência sem exceção.
    @Override
    @Transactional(readOnly = true)
    public Optional<ImportJob> loadById(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");

        return importJobRepository.findById(jobId)
                .map(PersistenceMapper::toDomain);
    }

    // Lista jobs elegíveis para retry (FAILED/COMPLETED_WITH_ERRORS) via query PAGINADA por status,
    // ordenando pelos mais antigos — evita findAll().stream() (que degrada com o crescimento da tabela).
    @Override
    @Transactional(readOnly = true)
    public List<ImportJob> loadJobsToRetry(int maxJobs) {
        int limit = maxJobs <= 0 ? 100 : maxJobs;

        return importJobRepository.findByStatusInOrderByUpdatedAtAsc(
                        List.of(ImportJobStatus.FAILED, ImportJobStatus.COMPLETED_WITH_ERRORS),
                        PageRequest.of(0, limit)
                ).stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }

}
