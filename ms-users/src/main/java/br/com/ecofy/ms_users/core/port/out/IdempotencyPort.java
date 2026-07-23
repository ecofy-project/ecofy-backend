package br.com.ecofy.ms_users.core.port.out;

import java.time.Duration;

public interface IdempotencyPort {

    IdempotencyOutcome registerOnce(String operation, String key, String requestHash, Duration ttl);
}