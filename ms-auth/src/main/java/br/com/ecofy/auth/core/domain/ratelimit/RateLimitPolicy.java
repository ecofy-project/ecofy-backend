package br.com.ecofy.auth.core.domain.ratelimit;

import java.time.Duration;
import java.util.Objects;

// Representa o limite e a janela aplicados a uma operação controlada.
public record RateLimitPolicy(
        String name,
        int limit,
        Duration window
) {

    public RateLimitPolicy {
        Objects.requireNonNull(
                name,
                "name must not be null"
        );
        Objects.requireNonNull(
                window,
                "window must not be null"
        );

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "name must not be blank"
            );
        }

        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "limit must be > 0"
            );
        }

        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(
                    "window must be > 0"
            );
        }
    }
}
