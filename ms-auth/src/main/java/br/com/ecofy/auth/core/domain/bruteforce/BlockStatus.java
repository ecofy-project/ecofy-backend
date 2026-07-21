package br.com.ecofy.auth.core.domain.bruteforce;

import java.time.Duration;

// Representa o bloqueio temporário associado a uma tentativa de autenticação.
public record BlockStatus(
        boolean blocked,
        Duration retryAfter
) {

    private static final BlockStatus NOT_BLOCKED =
            new BlockStatus(false, Duration.ZERO);

    public static BlockStatus notBlocked() {
        return NOT_BLOCKED;
    }

    public static BlockStatus blockedFor(
            Duration retryAfter
    ) {
        Duration safe =
                retryAfter == null || retryAfter.isNegative()
                        ? Duration.ZERO
                        : retryAfter;

        return new BlockStatus(true, safe);
    }
}
