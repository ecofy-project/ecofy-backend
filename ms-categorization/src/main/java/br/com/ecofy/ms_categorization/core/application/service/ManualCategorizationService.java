package br.com.ecofy.ms_categorization.core.application.service;

import br.com.ecofy.ms_categorization.adapters.out.messaging.dto.CategorizationAppliedEvent;
import br.com.ecofy.ms_categorization.adapters.out.messaging.dto.CategorizedTransactionEvent;
import br.com.ecofy.ms_categorization.core.application.command.ManualCategorizeCommand;
import br.com.ecofy.ms_categorization.core.application.exception.CategoryNotFoundException;
import br.com.ecofy.ms_categorization.core.application.exception.TransactionNotFoundException;
import br.com.ecofy.ms_categorization.core.application.result.CategorizationResult;
import br.com.ecofy.ms_categorization.core.domain.CategorizationSuggestion;
import br.com.ecofy.ms_categorization.core.domain.Transaction;
import br.com.ecofy.ms_categorization.core.domain.enums.SuggestionStatus;
import br.com.ecofy.ms_categorization.core.port.in.ManualCategorizationUseCase;
import br.com.ecofy.ms_categorization.core.port.out.LoadCategoriesPortOut;
import br.com.ecofy.ms_categorization.core.port.out.LoadTransactionPortOut;
import br.com.ecofy.ms_categorization.core.port.out.PublishCategorizedTransactionEventPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveSuggestionPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveTransactionPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManualCategorizationService implements ManualCategorizationUseCase {

    private static final String MODE_MANUAL = "MANUAL";
    private static final int MANUAL_SCORE = 100;

    private final LoadTransactionPortOut loadTransactionPort;
    private final SaveTransactionPortOut saveTransactionPort;
    private final LoadCategoriesPortOut loadCategoriesPort;
    private final SaveSuggestionPortOut saveSuggestionPort;
    private final PublishCategorizedTransactionEventPortOut publishPort;

    private final Clock clock = Clock.systemUTC();

    // Aplica categorização manual garantindo existência de transação/categoria, persistindo sugestão e publicando eventos.
    @Override
    @Transactional
    public CategorizationResult manualCategorize(ManualCategorizeCommand command) {
        validate(command);

        Instant now = Instant.now(clock);

        Transaction tx = loadTransactionPort.findById(command.transactionId())
                .orElseThrow(() -> new TransactionNotFoundException(command.transactionId()));

        if (tx.getCategoryId() != null && tx.getCategoryId().equals(command.categoryId())) {
            log.info("[ManualCategorizationService] - [manualCategorize] -> no-op (same category) txId={} categoryId={}",
                    tx.getId(), command.categoryId());
            return new CategorizationResult(tx.getId(), true, tx.getCategoryId(), null, MODE_MANUAL, MANUAL_SCORE);
        }

        loadCategoriesPort.findById(command.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

        Transaction updated = tx.withCategory(command.categoryId(), now);
        saveTransactionPort.save(updated);

        CategorizationSuggestion savedSuggestion = saveSuggestionPort.save(new CategorizationSuggestion(
                UUID.randomUUID(),
                updated.getId(),
                command.categoryId(),
                null,
                SuggestionStatus.APPLIED_MANUAL,
                MANUAL_SCORE,
                normalizeRationale(command.rationale()),
                now,
                now
        ));

        publishEvents(updated, savedSuggestion.getId(), now);

        log.info("[ManualCategorizationService] - [manualCategorize] -> applied txId={} categoryId={} suggestionId={}",
                updated.getId(), command.categoryId(), savedSuggestion.getId());

        return new CategorizationResult(
                updated.getId(),
                true,
                command.categoryId(),
                savedSuggestion.getId(),
                MODE_MANUAL,
                MANUAL_SCORE
        );
    }

    // Publica eventos de transação categorizada e auditoria de aplicação para consumo por serviços downstream.
    private void publishEvents(Transaction updated, UUID suggestionId, Instant now) {
        publishPort.publish(new CategorizedTransactionEvent(
                UUID.randomUUID(),
                updated.getId(),
                updated.getImportJobId(),
                updated.getExternalId(),
                updated.getTransactionDate(),
                updated.getMoney().getAmount(),
                updated.getMoney().getCurrency(),
                updated.getCategoryId(),
                MODE_MANUAL,
                now
        ));

        publishPort.publish(new CategorizationAppliedEvent(
                UUID.randomUUID(),
                updated.getId(),
                updated.getCategoryId(),
                null,
                MODE_MANUAL,
                MANUAL_SCORE,
                suggestionId,
                now
        ));
    }

    // Valida campos obrigatórios do comando para evitar execução com parâmetros nulos.
    private static void validate(ManualCategorizeCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.transactionId(), "command.transactionId must not be null");
        Objects.requireNonNull(command.categoryId(), "command.categoryId must not be null");
    }

    // Normaliza a justificativa removendo whitespace e convertendo vazio para null.
    private static String normalizeRationale(String rationale) {
        if (rationale == null) return null;
        String r = rationale.trim();
        return r.isEmpty() ? null : r;
    }

}
