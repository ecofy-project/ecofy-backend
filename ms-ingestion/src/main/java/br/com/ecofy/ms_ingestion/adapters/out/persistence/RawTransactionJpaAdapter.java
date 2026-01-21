package br.com.ecofy.ms_ingestion.adapters.out.persistence;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportFileEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportJobEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.RawTransactionEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportJobRepository;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.RawTransactionRepository;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.port.out.SaveRawTransactionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RawTransactionJpaAdapter implements SaveRawTransactionPort {

    private final RawTransactionRepository rawTransactionRepository;
    private final ImportJobRepository importJobRepository;

    public RawTransactionJpaAdapter(RawTransactionRepository rawTransactionRepository,
                                    ImportJobRepository importJobRepository) {
        this.rawTransactionRepository = Objects.requireNonNull(rawTransactionRepository, "rawTransactionRepository must not be null");
        this.importJobRepository = Objects.requireNonNull(importJobRepository, "importJobRepository must not be null");
    }

    // Persiste em lote transações brutas garantindo integridade (jobs/files existentes) e minimizando round-trips.
    @Override
    @Transactional
    public void saveAll(List<RawTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        // garante que todas têm sourceType
        for (RawTransaction tx : transactions) {
            TransactionSourceType sourceType = Objects.requireNonNull(
                    tx.sourceType(),
                    "TransactionSourceType must not be null for tx id=" + tx.id()
            );
            // aqui você poderia, por exemplo, logar casos MANUAL_ENTRY x FILE_CSV etc.
        }

        // Carrega todos os jobs envolvidos em um único round-trip
        Set<UUID> jobIds = transactions.stream()
                .map(RawTransaction::importJobId)
                .collect(Collectors.toSet());

        Map<UUID, ImportJobEntity> jobsById = importJobRepository.findAllById(jobIds)
                .stream()
                .collect(Collectors.toMap(ImportJobEntity::getId, Function.identity()));

        List<RawTransactionEntity> entities = new ArrayList<>(transactions.size());

        for (RawTransaction tx : transactions) {
            ImportJobEntity job = jobsById.get(tx.importJobId());
            if (job == null) {
                throw new IllegalArgumentException("ImportJob not found: " + tx.importJobId());
            }

            ImportFileEntity file = job.getImportFile();
            if (file == null) {
                throw new IllegalStateException(
                        "ImportJob " + job.getId() + " is not associated with an ImportFile"
                );
            }

            RawTransactionEntity entity = PersistenceMapper.toEntity(tx, job, file);
            entities.add(entity);
        }

        rawTransactionRepository.saveAll(entities);

        log.debug(
                "[RawTransactionJpaAdapter] - [saveAll] -> Persistidas {} transações (jobs={} sources={})",
                entities.size(),
                jobIds.size(),
                transactions.stream().map(t -> t.sourceType().name()).collect(Collectors.toSet())
        );
    }

}
