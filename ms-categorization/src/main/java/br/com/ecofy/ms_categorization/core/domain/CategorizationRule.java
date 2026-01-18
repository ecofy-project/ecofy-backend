package br.com.ecofy.ms_categorization.core.domain;

import br.com.ecofy.ms_categorization.core.domain.enums.RuleStatus;
import br.com.ecofy.ms_categorization.core.domain.valueobject.RuleCondition;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CategorizationRule {

    private final UUID id;
    private final UUID categoryId;
    private final String name;
    private final RuleStatus status;
    private final int priority;
    private final List<RuleCondition> conditions;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Representa uma regra de categorização imutável (categoria alvo, condições e metadados) garantindo invariantes básicas do domínio.
    public CategorizationRule(
            UUID id,
            UUID categoryId,
            String name,
            RuleStatus status,
            int priority,
            List<RuleCondition> conditions,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.conditions = Objects.requireNonNull(conditions, "conditions must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.priority = priority;

        if (this.name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    // Retorna o identificador único da regra.
    public UUID getId() {
        return id;
    }

    // Retorna o id da categoria que a regra aplica quando for satisfeita.
    public UUID getCategoryId() {
        return categoryId;
    }

    // Retorna o nome descritivo da regra (para auditoria e manutenção).
    public String getName() {
        return name;
    }

    // Retorna o status da regra (ex.: ACTIVE/INACTIVE) para controle de avaliação.
    public RuleStatus getStatus() {
        return status;
    }

    // Retorna a prioridade usada como critério de desempate/ordenação durante a avaliação.
    public int getPriority() {
        return priority;
    }

    // Retorna a lista de condições que devem ser satisfeitas para a regra "bater".
    public List<RuleCondition> getConditions() {
        return conditions;
    }

    // Retorna o timestamp de criação para rastreabilidade/auditoria.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Retorna o timestamp da última atualização para rastreabilidade/auditoria.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Compara regras por valor (campos relevantes do objeto) para consistência em coleções e testes.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategorizationRule that)) return false;
        return priority == that.priority &&
                id.equals(that.id) &&
                categoryId.equals(that.categoryId) &&
                name.equals(that.name) &&
                status == that.status &&
                conditions.equals(that.conditions) &&
                createdAt.equals(that.createdAt) &&
                updatedAt.equals(that.updatedAt);
    }

    // Gera hash consistente com equals para uso em estruturas baseadas em hashing.
    @Override
    public int hashCode() {
        return Objects.hash(id, categoryId, name, status, priority, conditions, createdAt, updatedAt);
    }

    // Fornece uma representação textual completa da regra para logs e debug.
    @Override
    public String toString() {
        return "CategorizationRule[" +
                "id=" + id +
                ", categoryId=" + categoryId +
                ", name=" + name +
                ", status=" + status +
                ", priority=" + priority +
                ", conditions=" + conditions +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ']';
    }

}
