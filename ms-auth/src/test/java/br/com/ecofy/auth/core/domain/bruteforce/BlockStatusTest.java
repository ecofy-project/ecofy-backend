package br.com.ecofy.auth.core.domain.bruteforce;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do estado de bloqueio temporário")
class BlockStatusTest {

    @Test
    @DisplayName("Deve retornar a instância compartilhada representando ausência de bloqueio")
    void notBlocked_chamadasRepetidas_deveRetornarInstanciaCompartilhada() {
        // Arrange
        Duration expectedRetryAfter = Duration.ZERO;

        // Act
        BlockStatus firstResult = BlockStatus.notBlocked();
        BlockStatus secondResult = BlockStatus.notBlocked();

        // Assert
        assertAll(
                () -> assertSame(
                        firstResult,
                        secondResult
                ),
                () -> assertFalse(firstResult.blocked()),
                () -> assertEquals(
                        expectedRetryAfter,
                        firstResult.retryAfter()
                )
        );
    }

    @Test
    @DisplayName("Deve criar bloqueio preservando uma duração positiva")
    void blockedFor_duracaoPositiva_deveCriarBloqueioComDuracaoInformada() {
        // Arrange
        Duration retryAfter = Duration.ofMinutes(5);

        // Act
        BlockStatus result = BlockStatus.blockedFor(retryAfter);

        // Assert
        assertAll(
                () -> assertTrue(result.blocked()),
                () -> assertSame(
                        retryAfter,
                        result.retryAfter()
                ),
                () -> assertEquals(
                        new BlockStatus(true, retryAfter),
                        result
                )
        );
    }

    @Test
    @DisplayName("Deve criar bloqueio mantendo a duração igual a zero")
    void blockedFor_duracaoZero_deveCriarBloqueioComDuracaoZero() {
        // Arrange
        Duration retryAfter = Duration.ZERO;

        // Act
        BlockStatus result = BlockStatus.blockedFor(retryAfter);

        // Assert
        assertAll(
                () -> assertTrue(result.blocked()),
                () -> assertEquals(
                        Duration.ZERO,
                        result.retryAfter()
                ),
                () -> assertNotSame(
                        BlockStatus.notBlocked(),
                        result
                )
        );
    }

    @Test
    @DisplayName("Deve normalizar uma duração negativa para zero")
    void blockedFor_duracaoNegativa_deveCriarBloqueioComDuracaoZero() {
        // Arrange
        Duration retryAfter = Duration.ofSeconds(-1);

        // Act
        BlockStatus result = BlockStatus.blockedFor(retryAfter);

        // Assert
        assertAll(
                () -> assertTrue(result.blocked()),
                () -> assertEquals(
                        Duration.ZERO,
                        result.retryAfter()
                )
        );
    }

    @Test
    @DisplayName("Deve normalizar uma duração nula para zero")
    void blockedFor_duracaoNula_deveCriarBloqueioComDuracaoZero() {
        // Arrange
        Duration retryAfter = null;

        // Act
        BlockStatus result = BlockStatus.blockedFor(retryAfter);

        // Assert
        assertAll(
                () -> assertTrue(result.blocked()),
                () -> assertEquals(
                        Duration.ZERO,
                        result.retryAfter()
                )
        );
    }
}
