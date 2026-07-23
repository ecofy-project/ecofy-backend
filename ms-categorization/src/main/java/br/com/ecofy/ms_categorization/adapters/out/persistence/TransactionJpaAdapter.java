package br.com.ecofy.ms_categorization.adapters.out.persistence;

import br.com.ecofy.ms_categorization.adapters.out.persistence.mapper.TransactionMapper;
import br.com.ecofy.ms_categorization.adapters.out.persistence.repository.TransactionRepository;
import br.com.ecofy.ms_categorization.core.domain.Transaction;
import br.com.ecofy.ms_categorization.core.port.out.LoadTransactionPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveTransactionPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Centraliza a persistência e a consulta das transações.
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionJpaAdapter implements LoadTransactionPortOut, SaveTransactionPortOut {

    private final TransactionRepository repo;
    private final TransactionMapper mapper;

    // Consulta uma transação pelo identificador informado.
    @Override
    public Optional<Transaction> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        log.debug("[TransactionJpaAdapter] - [findById] -> txId={}", id);

        return repo.findById(id)
                .map(mapper::toDomain);
    }

    // Consulta uma transação pela chave externa de importação.
    @Override
    public Optional<Transaction> findByExternalKey(UUID importJobId, String externalId) {
        Objects.requireNonNull(importJobId, "importJobId must not be null");
        Objects.requireNonNull(externalId, "externalId must not be null");

        log.debug("[TransactionJpaAdapter] - [findByExternalKey] -> importJobId={} externalId={}", importJobId, externalId);

        return repo.findByImportJobIdAndExternalId(importJobId, externalId)
                .map(mapper::toDomain);
    }

    // Persiste a transação e retorna o domínio reconstituído.
    @Override
    @Transactional
    public Transaction save(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");

        log.debug(
                "[TransactionJpaAdapter] - [save] -> Salvando transação txId={} importJobId={} externalId={} categoryId={}",
                transaction.getId(),
                transaction.getImportJobId(),
                transaction.getExternalId(),
                transaction.getCategoryId()
        );

        var savedEntity = repo.save(mapper.toEntity(transaction));

        log.info(
                "[TransactionJpaAdapter] - [save] -> Transaction persisted txId={} categoryId={} updatedAt={}",
                savedEntity.getId(),
                savedEntity.getCategoryId(),
                savedEntity.getUpdatedAt()
        );

        return mapper.toDomain(savedEntity);
    }
}
