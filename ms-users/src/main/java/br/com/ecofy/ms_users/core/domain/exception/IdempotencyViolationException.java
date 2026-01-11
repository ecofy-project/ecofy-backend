package br.com.ecofy.ms_users.core.domain.exception;

public class IdempotencyViolationException extends RuntimeException {
    public IdempotencyViolationException(String message) {
        super(message);
    }
}
