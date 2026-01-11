package br.com.ecofy.ms_users.core.domain.exception;

import java.util.UUID;

public class ConnectionNotFoundException extends RuntimeException {
    public ConnectionNotFoundException(UUID connectionId) {
        super("Connection not found: " + connectionId);
    }
}