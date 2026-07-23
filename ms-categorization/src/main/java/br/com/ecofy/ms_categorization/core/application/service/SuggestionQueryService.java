package br.com.ecofy.ms_categorization.core.application.service;

import br.com.ecofy.ms_categorization.core.application.exception.TransactionNotFoundException;
import br.com.ecofy.ms_categorization.core.application.result.SuggestionResult;
import br.com.ecofy.ms_categorization.core.port.in.GetSuggestionUseCase;
import br.com.ecofy.ms_categorization.core.port.out.LoadTransactionPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveSuggestionPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

// Centraliza a consulta das sugestões de categorização.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuggestionQueryService implements GetSuggestionUseCase {

    private final LoadTransactionPortOut loadTransactionPort;
    private final SaveSuggestionPortOut saveSuggestionPort;

    // Consulta a sugestão mais recente após validar a existência da transação.
    @Override
    public SuggestionResult getByTransactionId(UUID transactionId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");

        log.debug("[SuggestionQueryService] - [getByTransactionId] -> txId={}", transactionId);

        boolean exists = loadTransactionPort.findById(transactionId).isPresent();
        if (!exists) {
            log.info("[SuggestionQueryService] - [getByTransactionId] -> transaction not found txId={}", transactionId);
            throw new TransactionNotFoundException(transactionId);
        }

        return saveSuggestionPort.findByTransactionId(transactionId)
                .map(sug -> {
                    log.debug("[SuggestionQueryService] - [getByTransactionId] -> suggestion found sugId={} status={}",
                            sug.getId(), sug.getStatus());
                    return new SuggestionResult(
                            sug.getId(),
                            sug.getTransactionId(),
                            sug.getCategoryId(),
                            sug.getRuleId(),
                            sug.getStatus(),
                            sug.getScore(),
                            sug.getRationale()
                    );
                })
                .orElseGet(() -> {
                    log.debug("[SuggestionQueryService] - [getByTransactionId] -> no suggestion for txId={}", transactionId);
                    return null;
                });
    }
}
