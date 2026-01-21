package br.com.ecofy.ms_budgeting.core.application.exception;

public class BudgetingProcessingException extends BudgetingApplicationException {

    public BudgetingProcessingException(String message) {
        super("BUDGETING_PROCESSING_ERROR", message);
    }

    public BudgetingProcessingException(String message, Throwable cause) {
        super("BUDGETING_PROCESSING_ERROR", message, cause);
    }
}
