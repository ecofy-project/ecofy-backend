package br.com.ecofy.ms_budgeting.core.domain;

import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class BudgetAlert {

    private final UUID id;
    private final UUID budgetId;
    private final UUID consumptionId;
    private final AlertSeverity severity;
    private final String message;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final Instant createdAt;

    // Correção Dia 6 (item #10): dados de enriquecimento necessários ao evento BUDGET_ALERT
    // consumido pelo ms-notification (userId, categoryId, limit/consumed/pct e currency).
    // São opcionais no domínio (podem ser null quando o alerta é recarregado do banco,
    // pois não são persistidos); no fluxo de publicação são sempre preenchidos.
    private final UUID userId;
    private final UUID categoryId;
    private final BigDecimal limitAmount;
    private final BigDecimal consumedAmount;
    private final Integer consumedPct;
    private final String currency;

    // Construtor de compatibilidade (sem enriquecimento) — usado pela persistência/reload.
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

    // Construtor completo — usado na criação do alerta no fluxo de projeção, carregando os
    // dados necessários para a notificação (compatibilidade com ms-notification).
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

        // Campos de enriquecimento são opcionais (podem ser null no reload de persistência).
        this.userId = userId;
        this.categoryId = categoryId;
        this.limitAmount = limitAmount;
        this.consumedAmount = consumedAmount;
        this.consumedPct = consumedPct;
        this.currency = currency;
    }

    // Retorna o identificador único do alerta.
    public UUID getId() { return id; }

    // Retorna o identificador do budget ao qual o alerta pertence.
    public UUID getBudgetId() { return budgetId; }

    // Retorna o identificador do consumo que originou o alerta.
    public UUID getConsumptionId() { return consumptionId; }

    // Retorna a severidade do alerta (WARNING/CRITICAL/etc.).
    public AlertSeverity getSeverity() { return severity; }

    // Retorna a mensagem descritiva do alerta.
    public String getMessage() { return message; }

    // Retorna a data inicial do período do budget associado ao alerta.
    public LocalDate getPeriodStart() { return periodStart; }

    // Retorna a data final do período do budget associado ao alerta.
    public LocalDate getPeriodEnd() { return periodEnd; }

    // Retorna o instante em que o alerta foi criado.
    public Instant getCreatedAt() { return createdAt; }

    // Retorna o usuário dono do budget (enriquecimento p/ notificação; pode ser null no reload).
    public UUID getUserId() { return userId; }

    // Retorna a categoria do budget (enriquecimento p/ notificação; pode ser null no reload).
    public UUID getCategoryId() { return categoryId; }

    // Retorna o valor limite do budget (enriquecimento p/ notificação; pode ser null no reload).
    public BigDecimal getLimitAmount() { return limitAmount; }

    // Retorna o valor consumido no período (enriquecimento p/ notificação; pode ser null no reload).
    public BigDecimal getConsumedAmount() { return consumedAmount; }

    // Retorna o percentual consumido arredondado (enriquecimento p/ notificação; pode ser null no reload).
    public Integer getConsumedPct() { return consumedPct; }

    // Retorna o código da moeda (enriquecimento p/ notificação; pode ser null no reload).
    public String getCurrency() { return currency; }

}
