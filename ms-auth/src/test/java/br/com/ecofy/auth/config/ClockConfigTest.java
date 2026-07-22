package br.com.ecofy.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("Testes unitários da configuração do relógio")
class ClockConfigTest {

    @Test
    @DisplayName("Deve criar um relógio configurado com o fuso horário UTC")
    void clock_configuracaoCriada_deveRetornarRelogioUtc() {
        // Arrange
        ClockConfig config = new ClockConfig();

        // Act
        Clock result = config.clock();

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertSame(
                        ZoneOffset.UTC,
                        result.getZone()
                )
        );
    }

    @Test
    @DisplayName("Deve criar um relógio que acompanhe o instante atual do sistema")
    void clock_relogioUtc_deveRetornarInstanteAtualizado() {
        // Arrange
        ClockConfig config = new ClockConfig();
        Instant beforeCreation = Instant.now();

        // Act
        Clock result = config.clock();
        Instant clockInstant = result.instant();
        Instant afterReading = Instant.now();

        // Assert
        assertAll(
                () -> assertFalse(
                        clockInstant.isBefore(beforeCreation)
                ),
                () -> assertFalse(
                        clockInstant.isAfter(afterReading)
                )
        );
    }
}
