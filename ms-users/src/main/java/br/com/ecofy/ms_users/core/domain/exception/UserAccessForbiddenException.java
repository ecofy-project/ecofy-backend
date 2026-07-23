package br.com.ecofy.ms_users.core.domain.exception;

public class UserAccessForbiddenException extends RuntimeException {

    public UserAccessForbiddenException(String message) {
        super(message);
    }
}
