package br.com.ecofy.ms_budgeting.adapters.in.web.dto.request;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateBudgetRequest(

        @DecimalMin(value = "0.01")
        BigDecimal newLimitAmount,

        String currency,

        BudgetStatus status

) { }
