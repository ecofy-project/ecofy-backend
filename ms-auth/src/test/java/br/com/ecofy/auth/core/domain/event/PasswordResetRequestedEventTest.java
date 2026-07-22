package br.com.ecofy.auth.core.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do evento de solicitação de redefinição de senha")
class PasswordResetRequestedEventTest {

    @Test
    @DisplayName("Deve preservar todos os valores recebidos pelo construtor completo")
    void constructor_valoresInformados_devePreservarComponentesDoEvento() {
        // Arrange
        String resetToken = "reset-token";
        Instant occurredAt = Instant.parse("2026-07-22T12:00:00Z");

        // Act
        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        null,
                        resetToken,
                        occurredAt
                );

        PasswordResetRequestedEvent equivalentEvent =
                new PasswordResetRequestedEvent(
                        null,
                        resetToken,
                        occurredAt
                );

        PasswordResetRequestedEvent differentEvent =
                new PasswordResetRequestedEvent(
                        null,
                        "different-token",
                        occurredAt
                );

        // Assert
        assertAll(
                () -> assertNull(event.user()),
                () -> assertEquals(
                        resetToken,
                        event.resetToken()
                ),
                () -> assertEquals(
                        occurredAt,
                        event.occurredAt()
                ),
                () -> assertEquals(
                        equivalentEvent,
                        event
                ),
                () -> assertEquals(
                        equivalentEvent.hashCode(),
                        event.hashCode()
                ),
                () -> assertNotEquals(
                        differentEvent,
                        event
                ),
                () -> assertTrue(
                        event.toString()
                                .contains(resetToken)
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar valores nulos no construtor completo")
    void constructor_valoresNulos_deveCriarEventoComComponentesNulos() {
        // Arrange e Act
        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        null,
                        null,
                        null
                );

        // Assert
        assertAll(
                () -> assertNull(event.user()),
                () -> assertNull(event.resetToken()),
                () -> assertNull(event.occurredAt())
        );
    }

    @Test
    @DisplayName("Deve definir automaticamente o instante atual ao utilizar o construtor simplificado")
    void constructorSimplificado_tokenInformado_deveDefinirInstanteAtual() {
        // Arrange
        String resetToken = "reset-token";
        Instant beforeCreation = Instant.now();

        // Act
        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        null,
                        resetToken
                );

        Instant afterCreation = Instant.now();

        // Assert
        assertAll(
                () -> assertNull(event.user()),
                () -> assertEquals(
                        resetToken,
                        event.resetToken()
                ),
                () -> assertTrue(
                        !event.occurredAt().isBefore(beforeCreation)
                                && !event.occurredAt().isAfter(afterCreation)
                )
        );
    }
}
