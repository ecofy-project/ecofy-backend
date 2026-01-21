package br.com.ecofy.ms_budgeting.core.application.exception;

public class InvalidCurrencyCodeException extends BudgetingValidationException {

    public InvalidCurrencyCodeException(String code) {
        super("Invalid currency code: " + code);
    }

    public InvalidCurrencyCodeException(String code, Throwable cause) {
        super("Invalid currency code: " + code, cause);
    }
}
