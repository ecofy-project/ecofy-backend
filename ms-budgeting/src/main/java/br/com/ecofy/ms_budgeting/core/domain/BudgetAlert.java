package br.com.ecofy.ms_budgeting.core.domain;

import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

// Representa um alerta de orçamento com dados opcionais para notificação.
public class BudgetAlert {

    private final UUID id;
    private final UUID budgetId;
    private final UUID consumptionId;
    private final AlertSeverity severity;
    private final String message;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final Instant createdAt;

    private final UUID userId;
    private final UUID categoryId;
    private final BigDecimal limitAmount;
    private final BigDecimal consumedAmount;
    private final Integer consumedPct;
    private final String currency;

    public BudgetAlert(UUID id,
                       UUID budgetId,
                       UUID consumptionId,
                       AlertSeverity severity,
                       String message,
                       LocalDate periodStart,
                       LocalDate periodEnd,
                       Instant createdAt) {
        this(id, budgetId, consumptionId, severity, message, periodStart, periodEnd, createdAt,
                null, null, null, null, null, null);
    }

    public BudgetAlert(UUID id,
                       UUID budgetId,
                       UUID consumptionId,
                       AlertSeverity severity,
                       String message,
                       LocalDate periodStart,
                       LocalDate periodEnd,
                       Instant createdAt,
                       UUID userId,
                       UUID categoryId,
                       BigDecimal limitAmount,
                       BigDecimal consumedAmount,
                       Integer consumedPct,
                       String currency) {

        this.id = Objects.requireNonNull(id);
        this.budgetId = Objects.requireNonNull(budgetId);
        this.consumptionId = Objects.requireNonNull(consumptionId);
        this.severity = Objects.requireNonNull(severity);
        this.message = Objects.requireNonNull(message);
        this.periodStart = Objects.requireNonNull(periodStart);
        this.periodEnd = Objects.requireNonNull(periodEnd);
        this.createdAt = Objects.requireNonNull(createdAt);

        this.userId = userId;
        this.categoryId = categoryId;
        this.limitAmount = limitAmount;
        this.consumedAmount = consumedAmount;
        this.consumedPct = consumedPct;
        this.currency = currency;
    }

    public UUID getId() { return id; }

    public UUID getBudgetId() { return budgetId; }

    public UUID getConsumptionId() { return consumptionId; }

    public AlertSeverity getSeverity() { return severity; }

    public String getMessage() { return message; }

    public LocalDate getPeriodStart() { return periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }

    public Instant getCreatedAt() { return createdAt; }

    public UUID getUserId() { return userId; }

    public UUID getCategoryId() { return categoryId; }

    public BigDecimal getLimitAmount() { return limitAmount; }

    public BigDecimal getConsumedAmount() { return consumedAmount; }

    public Integer getConsumedPct() { return consumedPct; }

    public String getCurrency() { return currency; }
}
