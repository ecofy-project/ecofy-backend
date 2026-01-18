package br.com.ecofy.ms_budgeting.core.application.command;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateBudgetCommand (

        UUID budgetId,

        BigDecimal newLimitAmount,

        String currency,

        BudgetStatus status

) { }
