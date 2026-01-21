package br.com.ecofy.ms_budgeting.core.application.exception;

public abstract class BudgetingApplicationException extends RuntimeException {

    private final String code;

    protected BudgetingApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected BudgetingApplicationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
