package br.com.ecofy.ms_budgeting.core.domain;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.BudgetKey;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Representa um orçamento e centraliza suas regras de alteração.
public class Budget {

    private final UUID id;
    private final BudgetKey key;
    private final BudgetPeriodType periodType;
    private Money limit;
    private BudgetStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    // Controla a versão utilizada na concorrência otimista.
    private Long version;

    public Budget(UUID id,
                  BudgetKey key,
                  BudgetPeriodType periodType,
                  Money limit,
                  BudgetStatus status,
                  Instant createdAt,
                  Instant updatedAt) {
        this(id, key, periodType, limit, status, createdAt, updatedAt, null);
    }

    public Budget(UUID id,
                  BudgetKey key,
                  BudgetPeriodType periodType,
                  Money limit,
                  BudgetStatus status,
                  Instant createdAt,
                  Instant updatedAt,
                  Long version) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.periodType = Objects.requireNonNull(periodType, "periodType must not be null");
        this.limit = Objects.requireNonNull(limit, "limit must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.version = version;

        if (limit.amount().signum() <= 0) {
            throw new IllegalArgumentException("Budget limit must be greater than zero");
        }
    }

    public UUID getId() { return id; }

    public BudgetKey getKey() { return key; }

    public BudgetPeriodType getPeriodType() { return periodType; }

    public Money getLimit() { return limit; }

    public BudgetStatus getStatus() { return status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    // Atualiza o limite monetário e o instante da alteração.
    public void updateLimit(Money newLimit, Instant now) {
        Objects.requireNonNull(newLimit, "newLimit must not be null");
        if (newLimit.amount().signum() <= 0) throw new IllegalArgumentException("Budget limit must be greater than zero");
        this.limit = newLimit;
        this.updatedAt = Objects.requireNonNull(now);
    }

    // Atualiza o status e o instante da alteração.
    public void updateStatus(BudgetStatus newStatus, Instant now) {
        this.status = Objects.requireNonNull(newStatus, "newStatus must not be null");
        this.updatedAt = Objects.requireNonNull(now);
    }

    public boolean isActive() {
        return this.status == BudgetStatus.ACTIVE;
    }

    public Long getVersion() { return version; }

    // Aplica a versão esperada para detectar conflitos durante a persistência.
    public void applyExpectedVersion(Long expectedVersion) {
        if (expectedVersion != null) {
            this.version = expectedVersion;
        }
    }
}
