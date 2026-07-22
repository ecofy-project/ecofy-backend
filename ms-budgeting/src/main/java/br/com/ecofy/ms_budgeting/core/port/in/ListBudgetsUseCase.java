package br.com.ecofy.ms_budgeting.core.port.in;

import br.com.ecofy.ms_budgeting.core.application.result.BudgetResult;
import br.com.ecofy.ms_budgeting.core.port.out.PageResult;

import java.util.List;
import java.util.UUID;

public interface ListBudgetsUseCase {

    @Deprecated
    List<BudgetResult> listByUser(UUID userId);

    PageResult<BudgetResult> list(ListBudgetsQuery query);

    record ListBudgetsQuery(UUID ownerId, int page, int size, String sortField, boolean ascending) {}
}
