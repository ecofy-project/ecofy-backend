package br.com.ecofy.ms_users.core.application.pagination;

public class InvalidPaginationException extends RuntimeException {

    public InvalidPaginationException(String message) {
        super(message);
    }
}
