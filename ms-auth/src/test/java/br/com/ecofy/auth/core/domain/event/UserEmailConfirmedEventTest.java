package br.com.ecofy.auth.core.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do evento de confirmação de e-mail do usuário")
class UserEmailConfirmedEventTest {

    @Test
    @DisplayName("Deve preservar os valores recebidos pelo construtor completo")
    void constructorCompleto_valoresInformados_devePreservarComponentesDoEvento() {
        // Arrange
        Instant occurredAt = Instant.parse("2026-07-22T12:00:00Z");

        // Act
        UserEmailConfirmedEvent event =
                new UserEmailConfirmedEvent(
                        null,
                        occurredAt
                );

        UserEmailConfirmedEvent equivalentEvent =
                new UserEmailConfirmedEvent(
                        null,
                        occurredAt
                );

        UserEmailConfirmedEvent differentEvent =
                new UserEmailConfirmedEvent(
                        null,
                        occurredAt.plusSeconds(1)
                );

        // Assert
        assertAll(
                () -> assertNull(event.user()),
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
                () -> assertNotEquals(
                        null,
                        event
                ),
                () -> assertNotEquals(
                        "outro tipo",
                        event
                ),
                () -> assertTrue(
                        event.toString()
                                .contains(occurredAt.toString())
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar valores nulos no construtor completo")
    void constructorCompleto_valoresNulos_deveCriarEventoComComponentesNulos() {
        // Arrange e Act
        UserEmailConfirmedEvent event =
                new UserEmailConfirmedEvent(
                        null,
                        null
                );

        // Assert
        assertAll(
                () -> assertNull(event.user()),
                () -> assertNull(event.occurredAt())
        );
    }

    @Test
    @DisplayName("Deve definir automaticamente o instante atual ao utilizar o construtor simplificado")
    void constructorSimplificado_usuarioNulo_deveDefinirInstanteAtual() {
        // Arrange
        Instant beforeCreation = Instant.now();

        // Act
        UserEmailConfirmedEvent event =
                new UserEmailConfirmedEvent(null);

        Instant afterCreation = Instant.now();

        // Assert
        assertAll(
                () -> assertNull(event.user()),
                () -> assertNotNull(event.occurredAt()),
                () -> assertTrue(
                        !event.occurredAt().isBefore(beforeCreation)
                                && !event.occurredAt().isAfter(afterCreation)
                )
        );
    }
}
