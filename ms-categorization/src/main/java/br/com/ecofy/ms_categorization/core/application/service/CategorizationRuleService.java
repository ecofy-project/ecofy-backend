package br.com.ecofy.ms_categorization.core.application.service;

import br.com.ecofy.ms_categorization.core.application.command.CreateRuleCommand;
import br.com.ecofy.ms_categorization.core.application.exception.CategoryNotFoundException;
import br.com.ecofy.ms_categorization.core.domain.CategorizationRule;
import br.com.ecofy.ms_categorization.core.port.in.CreateRuleUseCase;
import br.com.ecofy.ms_categorization.core.port.in.ListRulesUseCase;
import br.com.ecofy.ms_categorization.core.port.out.LoadCategoriesPortOut;
import br.com.ecofy.ms_categorization.core.port.out.LoadRulesPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveRulePortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Centraliza a criação e a consulta das regras de categorização.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategorizationRuleService implements CreateRuleUseCase, ListRulesUseCase {

    private final SaveRulePortOut saveRulePort;
    private final LoadRulesPortOut loadRulesPort;
    private final LoadCategoriesPortOut loadCategoriesPort;

    private final Clock clock = Clock.systemUTC();

    // Registra uma regra após validar a existência da categoria.
    @Override
    @Transactional
    public CategorizationRule create(CreateRuleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.categoryId(), "command.categoryId must not be null");
        Objects.requireNonNull(command.name(), "command.name must not be null");
        Objects.requireNonNull(command.status(), "command.status must not be null");
        Objects.requireNonNull(command.conditions(), "command.conditions must not be null");

        loadCategoriesPort.findById(command.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(command.categoryId()));

        Instant now = Instant.now(clock);

        CategorizationRule rule = new CategorizationRule(
                UUID.randomUUID(),
                command.categoryId(),
                command.name(),
                command.status(),
                command.priority(),
                command.conditions(),
                now,
                now
        );

        CategorizationRule saved = saveRulePort.save(rule);

        log.info("[CategorizationRuleService] - [create] -> ruleId={} categoryId={} priority={} status={} conditions={}",
                saved.getId(), saved.getCategoryId(), saved.getPriority(), saved.getStatus(),
                saved.getConditions() != null ? saved.getConditions().size() : 0
        );

        return saved;
    }

    // Consulta as regras ativas conforme a prioridade definida.
    @Override
    public List<CategorizationRule> listActive() {
        log.debug("[CategorizationRuleService] - [listActive] -> Listing active rules ordered by priority");

        var rules = loadRulesPort.findActiveOrdered();

        log.debug("[CategorizationRuleService] - [listActive] -> loadedRules={}", rules.size());
        return rules;
    }
}
