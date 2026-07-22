package br.com.ecofy.ms_budgeting.core.application.exception;

public class InvalidCurrencyCodeException extends BudgetingValidationException {

    public InvalidCurrencyCodeException(String code) {
        super("Unsupported currency: " + code);
    }

    public InvalidCurrencyCodeException(String code, Throwable cause) {
        super("Unsupported currency: " + code, cause);
    }
}
