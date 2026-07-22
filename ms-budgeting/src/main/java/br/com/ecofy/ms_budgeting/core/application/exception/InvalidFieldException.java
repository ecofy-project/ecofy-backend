package br.com.ecofy.ms_budgeting.core.application.exception;

public class InvalidFieldException extends BudgetingValidationException {

    // Convenção oficial de mensagens (§4.2): Field '{field}' {problem}
    public InvalidFieldException(String field, String reason) {
        super("Field '" + field + "' " + reason);
    }

    public static InvalidFieldException required(String field) {
        return new InvalidFieldException(field, "must be provided");
    }

    public static InvalidFieldException notBlank(String field) {
        return new InvalidFieldException(field, "must not be blank");
    }

    public static InvalidFieldException invalid(String field, String reason) {
        return new InvalidFieldException(field, "is invalid: " + reason);
    }

}
