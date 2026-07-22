package br.com.ecofy.auth.core.domain.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários da decisão de limite de requisições")
class RateLimitDecisionTest {

    @Test
    @DisplayName("Deve retornar a decisão compartilhada que permite a requisição")
    void allow_semParametros_deveRetornarDecisaoPermitidaCompartilhada() {
        // Arrange e Act
        RateLimitDecision firstResult = RateLimitDecision.allow();
        RateLimitDecision secondResult = RateLimitDecision.allow();

        // Assert
        assertAll(
                () -> assertTrue(firstResult.allowed()),
                () -> assertEquals(
                        Duration.ZERO,
                        firstResult.retryAfter()
                ),
                () -> assertSame(
                        firstResult,
                        secondResult
                )
        );
    }

    @Test
    @DisplayName("Deve negar a requisição preservando uma duração positiva")
    void deny_duracaoPositiva_deveRetornarDecisaoNegadaComDuracaoInformada() {
        // Arrange
        Duration retryAfter = Duration.ofSeconds(30);

        // Act
        RateLimitDecision result =
                RateLimitDecision.deny(retryAfter);

        RateLimitDecision equivalentDecision =
                new RateLimitDecision(
                        false,
                        retryAfter
                );

        RateLimitDecision differentDecision =
                new RateLimitDecision(
                        false,
                        retryAfter.plusSeconds(1)
                );

        // Assert
        assertAll(
                () -> assertFalse(result.allowed()),
                () -> assertSame(
                        retryAfter,
                        result.retryAfter()
                ),
                () -> assertEquals(
                        equivalentDecision,
                        result
                ),
                () -> assertEquals(
                        equivalentDecision.hashCode(),
                        result.hashCode()
                ),
                () -> assertNotEquals(
                        differentDecision,
                        result
                ),
                () -> assertNotEquals(
                        null,
                        result
                ),
                () -> assertNotEquals(
                        "outro tipo",
                        result
                ),
                () -> assertTrue(
                        result.toString().contains("allowed=false")
                ),
                () -> assertTrue(
                        result.toString().contains(retryAfter.toString())
                )
        );
    }

    @Test
    @DisplayName("Deve negar a requisição preservando uma duração igual a zero")
    void deny_duracaoZero_deveRetornarDecisaoNegadaComDuracaoZero() {
        // Arrange
        Duration retryAfter = Duration.ZERO;

        // Act
        RateLimitDecision result =
                RateLimitDecision.deny(retryAfter);

        // Assert
        assertAll(
                () -> assertFalse(result.allowed()),
                () -> assertEquals(
                        Duration.ZERO,
                        result.retryAfter()
                ),
                () -> assertNotSame(
                        RateLimitDecision.allow(),
                        result
                )
        );
    }

    @Test
    @DisplayName("Deve normalizar uma duração negativa para zero ao negar a requisição")
    void deny_duracaoNegativa_deveRetornarDecisaoNegadaComDuracaoZero() {
        // Arrange
        Duration retryAfter = Duration.ofNanos(-1);

        // Act
        RateLimitDecision result =
                RateLimitDecision.deny(retryAfter);

        // Assert
        assertAll(
                () -> assertFalse(result.allowed()),
                () -> assertEquals(
                        Duration.ZERO,
                        result.retryAfter()
                )
        );
    }

    @Test
    @DisplayName("Deve normalizar uma duração nula para zero ao negar a requisição")
    void deny_duracaoNula_deveRetornarDecisaoNegadaComDuracaoZero() {
        // Arrange
        Duration retryAfter = null;

        // Act
        RateLimitDecision result =
                RateLimitDecision.deny(retryAfter);

        // Assert
        assertAll(
                () -> assertFalse(result.allowed()),
                () -> assertEquals(
                        Duration.ZERO,
                        result.retryAfter()
                )
        );
    }
}
