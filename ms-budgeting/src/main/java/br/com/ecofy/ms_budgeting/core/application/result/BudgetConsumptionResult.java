package br.com.ecofy.ms_budgeting.core.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetConsumptionResult (

        UUID budgetId,

        BigDecimal consumedAmount,

        BigDecimal limitAmount,

        BigDecimal consumedPct

) { }