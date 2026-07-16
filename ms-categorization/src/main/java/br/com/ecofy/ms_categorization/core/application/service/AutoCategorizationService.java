package br.com.ecofy.ms_categorization.core.application.service;

import br.com.ecofy.ms_categorization.config.CategorizationProperties;
import br.com.ecofy.ms_categorization.core.application.command.AutoCategorizeCommand;
import br.com.ecofy.ms_categorization.core.application.result.CategorizationResult;
import br.com.ecofy.ms_categorization.core.domain.CategorizationRule;
import br.com.ecofy.ms_categorization.core.domain.CategorizationSuggestion;
import br.com.ecofy.ms_categorization.core.domain.Transaction;
import br.com.ecofy.ms_categorization.core.domain.enums.SuggestionStatus;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizationAppliedDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizedTransactionDomainEvent;
import br.com.ecofy.ms_categorization.core.port.in.AutoCategorizeTransactionUseCase;
import br.com.ecofy.ms_categorization.core.port.out.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AutoCategorizationService implements AutoCategorizeTransactionUseCase {

    private final CategorizationProperties props;
    private final IdempotencyPortOut idempotencyPort;
    private final SaveTransactionPortOut saveTransactionPort;
    private final LoadRulesPortOut loadRulesPort;
    private final SaveSuggestionPortOut saveSuggestionPort;
    private final PublishCategorizedTransactionEventPortOut publishPort;

    private final Clock clock;
    private final RuleEngine engine;

    // Construtor padrão para wiring do Spring usando Clock/RuleEngine default.
    @Autowired
    public AutoCategorizationService(
            CategorizationProperties props,
            IdempotencyPortOut idempotencyPort,
            SaveTransactionPortOut saveTransactionPort,
            LoadRulesPortOut loadRulesPort,
            SaveSuggestionPortOut saveSuggestionPort,
            PublishCategorizedTransactionEventPortOut publishPort
    ) {
        this(props, idempotencyPort, saveTransactionPort, loadRulesPort, saveSuggestionPort, publishPort,
                Clock.systemUTC(), new RuleEngine());
    }

    // Inicializa e valida dependências obrigatórias do serviço.
    public AutoCategorizationService(
            CategorizationProperties props,
            IdempotencyPortOut idempotencyPort,
            SaveTransactionPortOut saveTransactionPort,
            LoadRulesPortOut loadRulesPort,
            SaveSuggestionPortOut saveSuggestionPort,
            PublishCategorizedTransactionEventPortOut publishPort,
            Clock clock,
            RuleEngine engine
    ) {
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        this.saveTransactionPort = Objects.requireNonNull(saveTransactionPort, "saveTransactionPort must not be null");
        this.loadRulesPort = Objects.requireNonNull(loadRulesPort, "loadRulesPort must not be null");
        this.saveSuggestionPort = Objects.requireNonNull(saveSuggestionPort, "saveSuggestionPort must not be null");
        this.publishPort = Objects.requireNonNull(publishPort, "publishPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    // Executa a auto-categorização com idempotência, avaliação de regras, persistência e publicação de eventos.
    @Override
    @Transactional
    public CategorizationResult autoCategorize(AutoCategorizeCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.inboundTransaction(), "command.inboundTransaction must not be null");

        Instant now = Instant.now(clock);
        String key = command.idempotencyKey();
        Transaction inbound = command.inboundTransaction();

        if (!idempotencyPort.tryAcquire(key, now)) {
            log.info("[AutoCategorizationService] - [autoCategorize] -> IDEMPOTENT_REPLAY key={} txId={}",
                    key, inbound.getId());

            CategorizationSuggestion existing =
                    saveSuggestionPort.findByTransactionId(inbound.getId()).orElse(null);

            boolean categorized = existing != null && existing.getCategoryId() != null;

            return new CategorizationResult(
                    inbound.getId(),
                    categorized,
                    existing != null ? existing.getCategoryId() : null,
                    existing != null ? existing.getId() : null,
                    "IDEMPOTENT_REPLAY",
                    existing != null ? existing.getScore() : 0
            );
        }

        Transaction savedTx = saveTransactionPort.save(inbound);

        List<CategorizationRule> rules = loadRulesPort.findActiveOrdered();

        int maxRules = Math.max(1, props.getRuleEngine().getMaxRulesToEvaluate());
        List<CategorizationRule> evaluated = rules.stream().limit(maxRules).toList();

        Candidate best = evaluated.stream()
                .map(rule -> toCandidate(savedTx, rule))
                .flatMap(Optional::stream)
                .max(bestCandidateComparator())
                .orElse(null);

        int minScore = props.getRuleEngine().getMinScoreToCategorize();
        boolean categorized = best != null && best.score() >= minScore;

        if (!categorized) {
            if (props.getRuleEngine().isCreateSuggestionWhenUnmatched()) {
                saveSuggestionPort.save(new CategorizationSuggestion(
                        UUID.randomUUID(),
                        savedTx.getId(),
                        null,
                        null,
                        SuggestionStatus.UNMATCHED,
                        0,
                        "no rule matched",
                        now,
                        now
                ));
            }

            log.info("[AutoCategorizationService] - [autoCategorize] -> UNMATCHED txId={} evaluatedRules={}",
                    savedTx.getId(), evaluated.size());

            return new CategorizationResult(savedTx.getId(), false, null, null, "UNMATCHED", 0);
        }

        CategorizationSuggestion savedSuggestion = saveSuggestionPort.save(new CategorizationSuggestion(
                UUID.randomUUID(),
                savedTx.getId(),
                best.categoryId(),
                best.ruleId(),
                SuggestionStatus.APPLIED_AUTO,
                best.score(),
                best.rationale(),
                now,
                now
        ));

        Transaction updatedTx = savedTx.withCategory(best.categoryId(), now);
        saveTransactionPort.save(updatedTx);

        publishPort.publish(new CategorizedTransactionDomainEvent(
                UUID.randomUUID(),
                updatedTx.getId(),
                updatedTx.getImportJobId(),
                updatedTx.getExternalId(),
                updatedTx.getTransactionDate(),
                updatedTx.getMoney().getAmount(),
                updatedTx.getMoney().getCurrency(),
                best.categoryId(),
                "AUTO",
                now
        ));

        publishPort.publish(new CategorizationAppliedDomainEvent(
                UUID.randomUUID(),
                updatedTx.getId(),
                best.categoryId(),
                best.ruleId(),
                "AUTO",
                best.score(),
                savedSuggestion.getId(),
                now
        ));

        log.info("[AutoCategorizationService] - [autoCategorize] -> CATEGORIZED txId={} categoryId={} ruleId={} score={}",
                updatedTx.getId(), best.categoryId(), best.ruleId(), best.score());

        return new CategorizationResult(
                updatedTx.getId(),
                true,
                best.categoryId(),
                savedSuggestion.getId(),
                "AUTO",
                best.score()
        );
    }

    // Calcula o melhor score da regra para a transação e retorna um candidato se houver match.
    private Optional<Candidate> toCandidate(Transaction tx, CategorizationRule rule) {
        var scores = engine.score(tx, rule);

        int bestScoreForRule = scores.max().orElse(0);
        if (bestScoreForRule <= 0) return Optional.empty();

        return Optional.of(new Candidate(
                rule.getId(),
                rule.getCategoryId(),
                bestScoreForRule,
                rule.getPriority(),
                "auto by best-score"
        ));
    }

    // Define a ordenação para escolher o melhor candidato (maior score; em empate, maior prioridade).
    private static Comparator<Candidate> bestCandidateComparator() {
        return Comparator.<Candidate>comparingInt(Candidate::score)
                .thenComparingInt(c -> -c.priority());
    }

    private record Candidate(UUID ruleId, UUID categoryId, int score, int priority, String rationale) { }

}
