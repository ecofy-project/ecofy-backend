package br.com.ecofy.ms_users.core.port.out;

import java.time.Duration;

public interface IdempotencyPort {

    /**
     * Registra (ou reconhece) uma chave de idempotência para uma operação, de forma atômica.
     *
     * @return {@link IdempotencyOutcome#REGISTERED} se registrou pela primeira vez;
     *         {@link IdempotencyOutcome#DUPLICATE} se já existia com o mesmo requestHash (retry legítimo);
     *         {@link IdempotencyOutcome#CONFLICT} se já existia com requestHash diferente (conflito real).
     */
    IdempotencyOutcome registerOnce(String operation, String key, String requestHash, Duration ttl);
}