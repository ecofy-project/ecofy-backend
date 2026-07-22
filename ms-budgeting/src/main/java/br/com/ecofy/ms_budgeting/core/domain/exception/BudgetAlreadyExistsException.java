package br.com.ecofy.ms_budgeting.core.domain.exception;

public class BudgetAlreadyExistsException extends RuntimeException {

    public BudgetAlreadyExistsException(String naturalKey) {
        super("Budget already exists for natural key: " + naturalKey);
    }

}
