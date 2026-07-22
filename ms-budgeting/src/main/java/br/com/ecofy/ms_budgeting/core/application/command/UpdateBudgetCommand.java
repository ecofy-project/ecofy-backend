package br.com.ecofy.ms_budgeting.core.application.command;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateBudgetCommand (

        UUID budgetId,

        BigDecimal newLimitAmount,

        String currency,

        BudgetStatus status,

        // Versão esperada pelo cliente (optimistic locking, ECO-11). Nullable: quando ausente,
        // aplica-se last-write sem detecção de conflito (Option A é opt-in por requisição).
        Long expectedVersion

) {
    // Construtor de compatibilidade (sem versão) — preserva chamadas existentes.
    public UpdateBudgetCommand(UUID budgetId, BigDecimal newLimitAmount, String currency, BudgetStatus status) {
        this(budgetId, newLimitAmount, currency, status, null);
    }
}
