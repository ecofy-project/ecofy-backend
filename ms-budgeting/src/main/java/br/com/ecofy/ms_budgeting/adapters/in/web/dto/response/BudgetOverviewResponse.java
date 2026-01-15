package br.com.ecofy.ms_budgeting.adapters.in.web.dto.response;

import br.com.ecofy.ms_budgeting.core.application.result.BudgetOverviewResult;

import java.util.List;
import java.util.UUID;

public record BudgetOverviewResponse(

        UUID userId,

        List<?> consumptions,

        List<?> alerts

) {

    public static BudgetOverviewResponse from(BudgetOverviewResult r) {
        return new BudgetOverviewResponse(r.userId(), r.consumptions(), r.alerts());
    }

}