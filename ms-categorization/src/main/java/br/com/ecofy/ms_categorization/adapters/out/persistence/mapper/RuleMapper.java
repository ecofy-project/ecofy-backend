package br.com.ecofy.ms_categorization.adapters.out.persistence.mapper;

import br.com.ecofy.ms_categorization.adapters.out.persistence.entity.CategorizationRuleEntity;
import br.com.ecofy.ms_categorization.core.domain.CategorizationRule;
import br.com.ecofy.ms_categorization.core.domain.valueobject.RuleCondition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class RuleMapper {

    private static final TypeReference<List<RuleCondition>> CONDITIONS_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Clock clock;

    // Construtor default que usa ObjectMapper padrão e Clock UTC.
    public RuleMapper() {
        this(new ObjectMapper(), Clock.systemUTC());
    }

    // Construtor que injeta ObjectMapper e usa Clock UTC.
    public RuleMapper(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemUTC());
    }

    // Construtor que injeta ObjectMapper e Clock para testes e consistência temporal.
    public RuleMapper(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Converte a entidade JPA CategorizationRuleEntity para o domínio CategorizationRule.
    public CategorizationRule toDomain(CategorizationRuleEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return new CategorizationRule(
                e.getId(),
                e.getCategoryId(),
                e.getName(),
                e.getStatus(),
                e.getPriority(),
                readConditions(e.getConditionsJson()),
                nonNullOrNow(e.getCreatedAt()),
                nonNullOrNow(e.getUpdatedAt())
        );
    }

    // Converte o domínio CategorizationRule para a entidade JPA CategorizationRuleEntity.
    public CategorizationRuleEntity toEntity(CategorizationRule d) {
        Objects.requireNonNull(d, "domain must not be null");

        return CategorizationRuleEntity.builder()
                .id(d.getId())
                .categoryId(d.getCategoryId())
                .name(d.getName())
                .status(d.getStatus())
                .priority(d.getPriority())
                .conditionsJson(writeConditions(d.getConditions()))
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    // Desserializa o JSON de condições da regra para uma lista de RuleCondition.
    private List<RuleCondition> readConditions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, CONDITIONS_TYPE);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize rule conditions JSON", ex);
        }
    }

    // Serializa a lista de RuleCondition para JSON para persistência.
    private String writeConditions(List<RuleCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(conditions);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize rule conditions to JSON", ex);
        }
    }

    // Retorna o Instant informado ou um Instant "agora" usando o Clock.
    private Instant nonNullOrNow(Instant value) {
        return value != null ? value : Instant.now(clock);
    }

}
