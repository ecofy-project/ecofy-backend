package br.com.ecofy.ms_budgeting.core.application.exception;

public class MissingIdempotencyKeyException extends BudgetingValidationException {

    public MissingIdempotencyKeyException() {
        super("Idempotency-Key header must be provided");
    }
}
