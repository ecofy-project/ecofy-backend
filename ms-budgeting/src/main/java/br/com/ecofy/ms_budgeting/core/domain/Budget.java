package br.com.ecofy.ms_budgeting.core.domain;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.BudgetKey;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Budget {

    private final UUID id;
    private final BudgetKey key;
    private final BudgetPeriodType periodType;
    private Money limit;
    private BudgetStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Budget(UUID id,
                  BudgetKey key,
                  BudgetPeriodType periodType,
                  Money limit,
                  BudgetStatus status,
                  Instant createdAt,
                  Instant updatedAt) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.periodType = Objects.requireNonNull(periodType, "periodType must not be null");
        this.limit = Objects.requireNonNull(limit, "limit must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        if (limit.amount().signum() <= 0) {
            throw new IllegalArgumentException("Budget limit must be > 0");
        }
    }

    // Retorna o identificador único do budget.
    public UUID getId() { return id; }

    // Retorna a chave natural do budget (user + category + período).
    public BudgetKey getKey() { return key; }

    // Retorna o tipo de período do budget (mensal/semanal/etc.).
    public BudgetPeriodType getPeriodType() { return periodType; }

    // Retorna o limite monetário configurado para o budget.
    public Money getLimit() { return limit; }

    // Retorna o status atual do budget (ACTIVE/ARCHIVED/etc.).
    public BudgetStatus getStatus() { return status; }

    // Retorna a data/hora de criação do budget.
    public Instant getCreatedAt() { return createdAt; }

    // Retorna a data/hora da última atualização do budget.
    public Instant getUpdatedAt() { return updatedAt; }

    // Atualiza o limite do budget e registra o instante da alteração.
    public void updateLimit(Money newLimit, Instant now) {
        Objects.requireNonNull(newLimit, "newLimit must not be null");
        if (newLimit.amount().signum() <= 0) throw new IllegalArgumentException("Budget limit must be > 0");
        this.limit = newLimit;
        this.updatedAt = Objects.requireNonNull(now);
    }

    // Atualiza o status do budget e registra o instante da alteração.
    public void updateStatus(BudgetStatus newStatus, Instant now) {
        this.status = Objects.requireNonNull(newStatus, "newStatus must not be null");
        this.updatedAt = Objects.requireNonNull(now);
    }

    // Indica se o budget está ativo e deve participar de projeções/cálculos.
    public boolean isActive() {
        return this.status == BudgetStatus.ACTIVE;
    }

}
