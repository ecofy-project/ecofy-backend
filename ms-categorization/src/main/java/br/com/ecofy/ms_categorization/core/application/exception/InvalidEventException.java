package br.com.ecofy.ms_categorization.core.application.exception;

public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String reason) {
        super(reason);
    }
}
