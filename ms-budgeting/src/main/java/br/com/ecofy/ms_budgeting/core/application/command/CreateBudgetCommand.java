package br.com.ecofy.ms_budgeting.core.application.command;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBudgetCommand (

        UUID userId,

        UUID categoryId,

        BudgetPeriodType periodType,

        LocalDate periodStart,

        LocalDate periodEnd,

        BigDecimal limitAmount,

        String currency,

        BudgetStatus status

) { }