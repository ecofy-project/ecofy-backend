package br.com.ecofy.ms_categorization.adapters.out.persistence;

import br.com.ecofy.ms_categorization.adapters.out.persistence.mapper.SuggestionMapper;
import br.com.ecofy.ms_categorization.adapters.out.persistence.repository.CategorizationSuggestionRepository;
import br.com.ecofy.ms_categorization.core.domain.CategorizationSuggestion;
import br.com.ecofy.ms_categorization.core.port.out.SaveSuggestionPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Centraliza a persistência e a consulta das sugestões de categorização.
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategorizationSuggestionJpaAdapter implements SaveSuggestionPortOut {

    private final CategorizationSuggestionRepository repo;
    private final SuggestionMapper mapper;

    // Persiste a sugestão e retorna o domínio reconstituído.
    @Override
    @Transactional
    public CategorizationSuggestion save(CategorizationSuggestion suggestion) {
        Objects.requireNonNull(suggestion, "suggestion must not be null");

        log.debug(
                "[CategorizationSuggestionJpaAdapter] - [save] -> Salvando sugestão txId={} categoryId={} status={} score={}",
                suggestion.getTransactionId(),
                suggestion.getCategoryId(),
                suggestion.getStatus(),
                suggestion.getScore()
        );

        var savedEntity = repo.save(mapper.toEntity(suggestion));

        log.info(
                "[CategorizationSuggestionJpaAdapter] - [save] -> Suggestion persisted suggestionId={} txId={} status={}",
                savedEntity.getId(),
                savedEntity.getTransactionId(),
                savedEntity.getStatus()
        );

        return mapper.toDomain(savedEntity);
    }

    // Consulta a sugestão mais recente associada à transação.
    @Override
    public Optional<CategorizationSuggestion> findByTransactionId(UUID transactionId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");

        log.debug(
                "[CategorizationSuggestionJpaAdapter] - [findByTransactionId] -> txId={}",
                transactionId
        );

        return repo.findTopByTransactionIdOrderByUpdatedAtDesc(transactionId)
                .map(mapper::toDomain);
    }
}