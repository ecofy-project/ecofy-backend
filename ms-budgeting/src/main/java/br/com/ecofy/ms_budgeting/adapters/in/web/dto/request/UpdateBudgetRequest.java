package br.com.ecofy.ms_budgeting.adapters.in.web.dto.request;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

// Representa os dados permitidos para atualização de um orçamento.
public record UpdateBudgetRequest(

        @DecimalMin(value = "0.01")
        BigDecimal newLimitAmount,

        String currency,

        BudgetStatus status,

        Long version

) {
    public UpdateBudgetRequest(BigDecimal newLimitAmount, String currency, BudgetStatus status) {
        this(newLimitAmount, currency, status, null);
    }
}
