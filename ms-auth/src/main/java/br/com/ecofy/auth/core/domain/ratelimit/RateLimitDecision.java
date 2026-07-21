package br.com.ecofy.auth.core.domain.ratelimit;

import java.time.Duration;

// Representa a decisão resultante do consumo de uma cota de requisições.
public record RateLimitDecision(
        boolean allowed,
        Duration retryAfter
) {

    private static final RateLimitDecision ALLOWED =
            new RateLimitDecision(true, Duration.ZERO);

    public static RateLimitDecision allow() {
        return ALLOWED;
    }

    public static RateLimitDecision deny(
            Duration retryAfter
    ) {
        Duration safe =
                retryAfter == null || retryAfter.isNegative()
                        ? Duration.ZERO
                        : retryAfter;

        return new RateLimitDecision(false, safe);
    }
}
