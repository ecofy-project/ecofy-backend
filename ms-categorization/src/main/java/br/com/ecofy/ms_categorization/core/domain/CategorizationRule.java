package br.com.ecofy.ms_categorization.core.domain;

import br.com.ecofy.ms_categorization.core.domain.enums.RuleStatus;
import br.com.ecofy.ms_categorization.core.domain.valueobject.RuleCondition;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Representa uma regra imutável usada na categorização de transações.
public final class CategorizationRule {

    private final UUID id;
    private final UUID categoryId;
    private final String name;
    private final RuleStatus status;
    private final int priority;
    private final List<RuleCondition> conditions;
    private final Instant createdAt;
    private final Instant updatedAt;

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

    public UUID getId() {
        return id;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public int getPriority() {
        return priority;
    }

    public List<RuleCondition> getConditions() {
        return conditions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(id, categoryId, name, status, priority, conditions, createdAt, updatedAt);
    }

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
