package br.com.ecofy.ms_budgeting.core.application.exception;

public class BudgetingValidationException extends BudgetingApplicationException {

    public BudgetingValidationException(String message) {
        super("BUDGETING_VALIDATION_ERROR", message);
    }

    public BudgetingValidationException(String message, Throwable cause) {
        super("BUDGETING_VALIDATION_ERROR", message, cause);
    }
}
