package br.com.ecofy.ms_ingestion.adapters.out.persistence;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportFileEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportJobEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportFileRepository;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportJobRepository;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportAlreadyProcessedException;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportFileNotFoundException;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportFilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Centraliza a persistência e a consulta dos arquivos de importação.
@Slf4j
@Component
public class ImportFileJpaAdapter implements SaveImportFilePort {

    private final ImportFileRepository repository;
    private final ImportJobRepository jobRepository;

    public ImportFileJpaAdapter(ImportFileRepository repository, ImportJobRepository jobRepository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository must not be null");
    }

    // Persiste o arquivo e traduz colisões de idempotência.
    @Override
    public ImportFile save(ImportFile file) {
        Objects.requireNonNull(file, "file must not be null");

        ImportFileType type = Objects.requireNonNull(
                file.type(), "ImportFileType (type) must not be null for file id=" + file.id());

        try {
            ImportFileEntity saved = repository.saveAndFlush(PersistenceMapper.toEntity(file));

            log.debug("[ImportFileJpaAdapter] - [save] -> ImportFile persistido id={} type={}", saved.getId(), type);
            return PersistenceMapper.toDomain(saved);

        } catch (DataIntegrityViolationException e) {
            log.info("[ImportFileJpaAdapter] - [save] -> Constraint de idempotência barrou upload duplicado userId={}",
                    file.userId());
            throw toAlreadyProcessed(file, e);
        }
    }

    // Resolve a colisão para o job já associado ao mesmo conteúdo.
    private RuntimeException toAlreadyProcessed(ImportFile file, DataIntegrityViolationException e) {
        Optional<ImportFileEntity> existing = repository.findByUserIdAndFileHash(file.userId(), file.fileHash());
        if (existing.isEmpty()) {
            return e;
        }

        UUID existingJobId = jobRepository
                .findFirstByImportFileIdOrderByCreatedAtAsc(existing.get().getId())
                .map(ImportJobEntity::getId)
                .orElse(null);

        return new ImportAlreadyProcessedException(existingJobId, file.fileHash());
    }

    @Override
    @Transactional(readOnly = true)
    public ImportFile getById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        ImportFileEntity entity = repository.findById(id)
                .orElseThrow(() -> new ImportFileNotFoundException(id));

        return PersistenceMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportFile> findByOwnerAndHash(UUID ownerId, String fileHash) {
        return repository.findByUserIdAndFileHash(ownerId, fileHash).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportFile> findByOwnerAndIdempotencyKey(UUID ownerId, String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return repository.findByUserIdAndIdempotencyKey(ownerId, idempotencyKey).map(PersistenceMapper::toDomain);
    }
}
