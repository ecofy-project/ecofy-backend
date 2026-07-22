package br.com.ecofy.ms_budgeting.core.domain;

import br.com.ecofy.ms_budgeting.core.domain.enums.ConsumptionSource;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

// Representa o consumo acumulado de um orçamento em determinado período.
public class BudgetConsumption {

    private final UUID id;
    private final UUID budgetId;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private Money consumed;
    private final ConsumptionSource source;
    private final Instant createdAt;
    private Instant updatedAt;

    public BudgetConsumption(UUID id,
                             UUID budgetId,
                             LocalDate periodStart,
                             LocalDate periodEnd,
                             Money consumed,
                             ConsumptionSource source,
                             Instant createdAt,
                             Instant updatedAt) {

        this.id = Objects.requireNonNull(id);
        this.budgetId = Objects.requireNonNull(budgetId);
        this.periodStart = Objects.requireNonNull(periodStart);
        this.periodEnd = Objects.requireNonNull(periodEnd);
        this.consumed = Objects.requireNonNull(consumed);
        this.source = Objects.requireNonNull(source);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public UUID getId() { return id; }

    public UUID getBudgetId() { return budgetId; }

    public LocalDate getPeriodStart() { return periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }

    public Money getConsumed() { return consumed; }

    public ConsumptionSource getSource() { return source; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    // Acumula um valor monetário e registra o instante da alteração.
    public void add(Money amount, Instant now) {
        this.consumed = this.consumed.plus(amount);
        this.updatedAt = Objects.requireNonNull(now);
    }
}
