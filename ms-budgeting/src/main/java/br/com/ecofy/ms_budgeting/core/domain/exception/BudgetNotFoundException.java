package br.com.ecofy.ms_budgeting.core.domain.exception;

import java.util.UUID;

public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException(UUID id) {
        super("Budget not found for id: " + id);
    }

}