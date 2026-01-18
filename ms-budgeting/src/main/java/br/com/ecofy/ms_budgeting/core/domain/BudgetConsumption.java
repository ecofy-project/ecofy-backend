package br.com.ecofy.ms_budgeting.core.domain;

import br.com.ecofy.ms_budgeting.core.domain.enums.ConsumptionSource;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

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

    // Retorna o identificador único do consumo.
    public UUID getId() { return id; }

    // Retorna o identificador do budget ao qual este consumo pertence.
    public UUID getBudgetId() { return budgetId; }

    // Retorna a data inicial do período de consumo consolidado.
    public LocalDate getPeriodStart() { return periodStart; }

    // Retorna a data final do período de consumo consolidado.
    public LocalDate getPeriodEnd() { return periodEnd; }

    // Retorna o valor consumido consolidado no período.
    public Money getConsumed() { return consumed; }

    // Retorna a origem do consumo (ex.: transação categorizada).
    public ConsumptionSource getSource() { return source; }

    // Retorna o instante de criação do registro de consumo.
    public Instant getCreatedAt() { return createdAt; }

    // Retorna o instante da última atualização do consumo.
    public Instant getUpdatedAt() { return updatedAt; }

    // Soma um valor ao consumo e atualiza o timestamp de atualização.
    public void add(Money amount, Instant now) {
        this.consumed = this.consumed.plus(amount);
        this.updatedAt = Objects.requireNonNull(now);
    }

}
