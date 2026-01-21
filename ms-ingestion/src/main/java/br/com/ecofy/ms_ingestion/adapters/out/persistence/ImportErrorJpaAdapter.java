package br.com.ecofy.ms_ingestion.adapters.out.persistence;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportErrorEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportJobEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportErrorRepository;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportJobRepository;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportErrorType;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportErrorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ImportErrorJpaAdapter implements SaveImportErrorPort {

    private final ImportErrorRepository errorRepository;
    private final ImportJobRepository jobRepository;

    public ImportErrorJpaAdapter(ImportErrorRepository errorRepository,
                                 ImportJobRepository jobRepository) {
        this.errorRepository = Objects.requireNonNull(errorRepository, "errorRepository must not be null");
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository must not be null");
    }

    // Persiste em lote os erros de importação, carregando os jobs relacionados em um único round-trip.
    @Override
    @Transactional
    public void saveAll(List<ImportError> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        // garantia extra: todos erros têm tipo
        for (ImportError error : errors) {
            ImportErrorType type = Objects.requireNonNull(
                    error.errorType(),
                    "ImportErrorType must not be null for error id=" + error.id()
            );
        }

        // Carrega todos os jobs envolvidos em um único round-trip
        Set<UUID> jobIds = errors.stream()
                .map(ImportError::importJobId)
                .collect(Collectors.toSet());

        Map<UUID, ImportJobEntity> jobsById = jobRepository.findAllById(jobIds)
                .stream()
                .collect(Collectors.toMap(ImportJobEntity::getId, Function.identity()));

        List<ImportErrorEntity> entities = new ArrayList<>(errors.size());

        for (ImportError error : errors) {
            ImportJobEntity jobEntity = jobsById.get(error.importJobId());
            if (jobEntity == null) {
                throw new IllegalArgumentException("ImportJob not found: " + error.importJobId());
            }

            ImportErrorEntity entity = PersistenceMapper.toEntity(error, jobEntity);
            entities.add(entity);
        }

        errorRepository.saveAll(entities);

        log.debug(
                "[ImportErrorJpaAdapter] - [saveAll] -> Persistidos {} erros para {} jobs (tipos={})",
                entities.size(),
                jobIds.size(),
                errors.stream().map(e -> e.errorType().name()).collect(Collectors.toSet())
        );
    }

}
