package br.com.ecofy.ms_budgeting.adapters.out.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Transporta o conteúdo de negócio do alerta de orçamento, com valores monetários em decimal.
public record BudgetAlertDataV1(
        UUID userId,
        UUID budgetId,
        UUID categoryId,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal limitAmount,
        BigDecimal consumedAmount,
        BigDecimal percentageConsumed,
        String currency,
        String alertLevel
) {
}
