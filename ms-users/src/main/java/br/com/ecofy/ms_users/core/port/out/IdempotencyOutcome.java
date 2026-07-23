package br.com.ecofy.ms_users.core.port.out;

public enum IdempotencyOutcome {

    REGISTERED,
    DUPLICATE,
    CONFLICT
}
