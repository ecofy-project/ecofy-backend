package br.com.ecofy.ms_categorization.adapters.out.persistence;

import br.com.ecofy.ms_categorization.adapters.out.persistence.mapper.RuleMapper;
import br.com.ecofy.ms_categorization.adapters.out.persistence.repository.CategorizationRuleRepository;
import br.com.ecofy.ms_categorization.core.domain.CategorizationRule;
import br.com.ecofy.ms_categorization.core.domain.enums.RuleStatus;
import br.com.ecofy.ms_categorization.core.port.out.LoadRulesPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveRulePortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategorizationRuleJpaAdapter implements LoadRulesPortOut, SaveRulePortOut {

    private final CategorizationRuleRepository repo;
    private final RuleMapper mapper;

    // Busca regras ativas ordenadas por prioridade (ascendente).
    @Override
    public List<CategorizationRule> findActiveOrdered() {
        log.debug("[CategorizationRuleJpaAdapter] - [findActiveOrdered] -> Loading ACTIVE rules ordered by priority");

        return repo.findByStatusOrderByPriorityAsc(RuleStatus.ACTIVE)
                .stream()
                .map(e -> {
                    try {
                        return mapper.toDomain(e);
                    } catch (Exception ex) {
                        log.error("[CategorizationRuleJpaAdapter] - [findActiveOrdered] -> Failed mapping ruleId={} conditionsJson={}",
                                e.getId(), e.getConditionsJson(), ex);
                        throw ex;
                    }
                })
                .toList();
    }


    // Busca uma regra por id e a converte para o domínio.
    @Override
    public Optional<CategorizationRule> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        log.debug("[CategorizationRuleJpaAdapter] - [findById] -> ruleId={}", id);

        return repo.findById(id).map(mapper::toDomain);
    }

    // Persiste uma regra (create/update) e retorna a versão do domínio salva.
    @Override
    @Transactional
    public CategorizationRule save(CategorizationRule rule) {
        Objects.requireNonNull(rule, "rule must not be null");

        log.debug(
                "[CategorizationRuleJpaAdapter] - [save] -> Saving rule name={} categoryId={} priority={} status={}",
                rule.getName(),
                rule.getCategoryId(),
                rule.getPriority(),
                rule.getStatus()
        );

        // Persiste entidade (grava conditions_json via writeConditions)
        var savedEntity = repo.save(mapper.toEntity(rule));

        log.info(
                "[CategorizationRuleJpaAdapter] - [save] -> Rule persisted ruleId={} categoryId={} priority={}",
                savedEntity.getId(),
                savedEntity.getCategoryId(),
                savedEntity.getPriority()
        );

        return new CategorizationRule(
                savedEntity.getId(),
                savedEntity.getCategoryId(),
                savedEntity.getName(),
                savedEntity.getStatus(),
                savedEntity.getPriority(),
                (rule.getConditions() != null ? rule.getConditions() : List.of()),
                (savedEntity.getCreatedAt() != null ? savedEntity.getCreatedAt() : rule.getCreatedAt()),
                (savedEntity.getUpdatedAt() != null ? savedEntity.getUpdatedAt() : rule.getUpdatedAt())
        );
    }

}
