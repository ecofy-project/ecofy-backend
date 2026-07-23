package br.com.ecofy.ms_ingestion.adapters.out.persistence;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportErrorEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportJobEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportErrorRepository;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportJobRepository;
import br.com.ecofy.ms_ingestion.core.application.exception.PersistenceException;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.port.out.LoadImportErrorPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportErrorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Centraliza a persistência e a consulta dos erros de importação.
@Slf4j
@Component
public class ImportErrorJpaAdapter implements SaveImportErrorPort, LoadImportErrorPort {

    private final ImportErrorRepository errorRepository;
    private final ImportJobRepository jobRepository;

    public ImportErrorJpaAdapter(ImportErrorRepository errorRepository,
                                 ImportJobRepository jobRepository) {
        this.errorRepository = Objects.requireNonNull(errorRepository, "errorRepository must not be null");
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository must not be null");
    }

    // Persiste os erros em lote com seus respectivos jobs.
    @Override
    @Transactional
    public void saveAll(List<ImportError> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        Set<UUID> jobIds = errors.stream().map(ImportError::importJobId).collect(Collectors.toSet());

        Map<UUID, ImportJobEntity> jobsById = jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(ImportJobEntity::getId, Function.identity()));

        List<ImportErrorEntity> entities = new ArrayList<>(errors.size());
        for (ImportError error : errors) {
            ImportJobEntity jobEntity = jobsById.get(error.importJobId());
            if (jobEntity == null) {
                throw new PersistenceException("Import job not found for id: " + error.importJobId(), null);
            }
            entities.add(PersistenceMapper.toEntity(error, jobEntity));
        }

        errorRepository.saveAll(entities);

        log.debug("[ImportErrorJpaAdapter] - [saveAll] -> Persistidos {} erros para {} jobs",
                entities.size(), jobIds.size());
    }

    // Consulta os primeiros erros do job conforme o limite informado.
    @Override
    @Transactional(readOnly = true)
    public List<ImportError> findByJobId(UUID jobId, int limit) {
        Objects.requireNonNull(jobId, "jobId must not be null");

        return errorRepository
                .findByImportJobIdOrderByLineNumberAsc(jobId, PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }
}
