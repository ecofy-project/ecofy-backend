package br.com.ecofy.ms_ingestion.adapters.out.persistence;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportFileEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportJobEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportFileRepository;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportJobRepository;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportFileNotFoundException;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import br.com.ecofy.ms_ingestion.core.port.out.LoadImportJobPort;
import br.com.ecofy.ms_ingestion.core.port.out.PageResult;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportJobPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Centraliza a persistência e a consulta dos jobs de importação.
@Component
public class ImportJobJpaAdapter implements SaveImportJobPort, LoadImportJobPort {

    // Restringe a ordenação às propriedades permitidas pela API.
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "createdAt", "createdAt",
            "finishedAt", "finishedAt",
            "status", "status",
            "fileName", "importFile.originalFilename"
    );

    private final ImportJobRepository importJobRepository;
    private final ImportFileRepository importFileRepository;

    public ImportJobJpaAdapter(ImportJobRepository importJobRepository,
                               ImportFileRepository importFileRepository) {
        this.importJobRepository = Objects.requireNonNull(importJobRepository, "importJobRepository must not be null");
        this.importFileRepository =
                Objects.requireNonNull(importFileRepository, "importFileRepository must not be null");
    }

    // Persiste o job após validar a existência do arquivo associado.
    @Override
    @Transactional
    public ImportJob save(ImportJob job) {
        Objects.requireNonNull(job, "job must not be null");

        ImportFileEntity fileEntity = importFileRepository.findById(job.importFileId())
                .orElseThrow(() -> new ImportFileNotFoundException(job.importFileId()));

        ImportJobEntity entity = PersistenceMapper.toEntity(job, fileEntity);
        return PersistenceMapper.toDomain(importJobRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportJob> loadById(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        return importJobRepository.findById(jobId).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportJob> findByImportFileId(UUID importFileId) {
        Objects.requireNonNull(importFileId, "importFileId must not be null");
        return importJobRepository.findFirstByImportFileIdOrderByCreatedAtAsc(importFileId)
                .map(PersistenceMapper::toDomain);
    }

    // Consulta os jobs elegíveis para nova tentativa em ordem cronológica.
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

    // Consulta o histórico paginado pertencente ao usuário.
    @Override
    @Transactional(readOnly = true)
    public PageResult<ImportJob> findByOwner(UUID ownerId,
                                             ImportJobStatus statusFilter,
                                             int page,
                                             int size,
                                             String sortField,
                                             boolean ascending) {

        Objects.requireNonNull(ownerId, "ownerId must not be null");

        String property = SORT_PROPERTIES.get(sortField);
        if (property == null) {
            throw new IllegalArgumentException("Sort field not allowed: " + sortField);
        }

        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, property);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<ImportJobEntity> result = statusFilter == null
                ? importJobRepository.findByUserId(ownerId, pageRequest)
                : importJobRepository.findByUserIdAndStatus(ownerId, statusFilter, pageRequest);

        return new PageResult<>(
                result.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                page,
                size,
                result.getTotalElements());
    }
}
