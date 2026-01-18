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

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionJpaAdapter implements LoadTransactionPortOut, SaveTransactionPortOut {

    private final TransactionRepository repo;
    private final TransactionMapper mapper;

    // Carrega uma transação por ID (UUID) e converte a entidade persistida para o domínio.
    @Override
    public Optional<Transaction> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        log.debug("[TransactionJpaAdapter] - [findById] -> txId={}", id);

        return repo.findById(id)
                .map(mapper::toDomain);
    }

    // Carrega uma transação pela chave externa (importJobId + externalId) e converte para o domínio.
    @Override
    public Optional<Transaction> findByExternalKey(UUID importJobId, String externalId) {
        Objects.requireNonNull(importJobId, "importJobId must not be null");
        Objects.requireNonNull(externalId, "externalId must not be null");

        log.debug("[TransactionJpaAdapter] - [findByExternalKey] -> importJobId={} externalId={}", importJobId, externalId);

        return repo.findByImportJobIdAndExternalId(importJobId, externalId)
                .map(mapper::toDomain);
    }

    // Persiste uma transação no banco via JPA e retorna o objeto de domínio correspondente ao registro salvo.
    @Override
    @Transactional
    public Transaction save(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");

        log.debug(
                "[TransactionJpaAdapter] - [save] -> Saving txId={} importJobId={} externalId={} categoryId={}",
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
