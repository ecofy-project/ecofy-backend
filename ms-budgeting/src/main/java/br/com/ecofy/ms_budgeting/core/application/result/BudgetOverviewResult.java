package br.com.ecofy.ms_budgeting.core.application.result;

import java.util.List;
import java.util.UUID;

public record BudgetOverviewResult(

        UUID userId,

        List<BudgetConsumptionResult> consumptions,

        List<BudgetAlertResult> alerts

) { }