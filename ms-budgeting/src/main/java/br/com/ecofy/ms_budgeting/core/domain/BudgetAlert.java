package br.com.ecofy.ms_budgeting.core.domain;

import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;

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

    public BudgetAlert(UUID id,
                       UUID budgetId,
                       UUID consumptionId,
                       AlertSeverity severity,
                       String message,
                       LocalDate periodStart,
                       LocalDate periodEnd,
                       Instant createdAt) {

        this.id = Objects.requireNonNull(id);
        this.budgetId = Objects.requireNonNull(budgetId);
        this.consumptionId = Objects.requireNonNull(consumptionId);
        this.severity = Objects.requireNonNull(severity);
        this.message = Objects.requireNonNull(message);
        this.periodStart = Objects.requireNonNull(periodStart);
        this.periodEnd = Objects.requireNonNull(periodEnd);
        this.createdAt = Objects.requireNonNull(createdAt);
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

}
