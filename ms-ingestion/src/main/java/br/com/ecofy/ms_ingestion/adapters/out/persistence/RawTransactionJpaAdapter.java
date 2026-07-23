package br.com.ecofy.ms_ingestion.adapters.out.persistence;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportFileEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportJobEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.RawTransactionEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportFileRepository;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.ImportJobRepository;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.repository.RawTransactionRepository;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportFileNotFoundException;
import br.com.ecofy.ms_ingestion.core.application.exception.PersistenceException;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.port.out.SaveRawTransactionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// Centraliza a persistência das transações brutas importadas.
@Slf4j
@Component
public class RawTransactionJpaAdapter implements SaveRawTransactionPort {

    private final RawTransactionRepository rawTransactionRepository;
    private final ImportJobRepository importJobRepository;
    private final ImportFileRepository importFileRepository;

    public RawTransactionJpaAdapter(RawTransactionRepository rawTransactionRepository,
                                    ImportJobRepository importJobRepository,
                                    ImportFileRepository importFileRepository) {
        this.rawTransactionRepository =
                Objects.requireNonNull(rawTransactionRepository, "rawTransactionRepository must not be null");
        this.importJobRepository =
                Objects.requireNonNull(importJobRepository, "importJobRepository must not be null");
        this.importFileRepository =
                Objects.requireNonNull(importFileRepository, "importFileRepository must not be null");
    }

    // Persiste o lote ignorando transações duplicadas no arquivo ou no próprio lote.
    @Override
    @Transactional
    public List<RawTransaction> saveBatch(UUID importFileId, List<RawTransaction> transactions) {
        Objects.requireNonNull(importFileId, "importFileId must not be null");

        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        Set<String> batchHashes = new HashSet<>(transactions.size());
        for (RawTransaction tx : transactions) {
            batchHashes.add(tx.rowHash());
        }

        Set<String> seen = new HashSet<>(
                rawTransactionRepository.findExistingRowHashes(importFileId, batchHashes));

        ImportFileEntity fileEntity = importFileRepository.findById(importFileId)
                .orElseThrow(() -> new ImportFileNotFoundException(importFileId));

        List<RawTransaction> inserted = new ArrayList<>(transactions.size());
        List<RawTransactionEntity> entities = new ArrayList<>(transactions.size());

        for (RawTransaction tx : transactions) {
            if (!seen.add(tx.rowHash())) {
                continue;
            }

            ImportJobEntity jobEntity = importJobRepository.findById(tx.importJobId())
                    .orElseThrow(() -> new PersistenceException(
                            "ImportJob not found: " + tx.importJobId(), null));

            entities.add(PersistenceMapper.toEntity(tx, jobEntity, fileEntity));
            inserted.add(tx);
        }

        if (entities.isEmpty()) {
            return List.of();
        }

        rawTransactionRepository.saveAll(entities);

        log.debug("[RawTransactionJpaAdapter] - [saveBatch] -> Lote persistido fileId={} recebidas={} inseridas={}",
                importFileId, transactions.size(), entities.size());

        return List.copyOf(inserted);
    }
}
