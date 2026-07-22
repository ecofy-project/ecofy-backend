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

@DisplayName("Testes unitários do evento de autenticação de usuário")
class UserAuthenticatedEventTest {

    @Test
    @DisplayName("Deve preservar todos os valores recebidos pelo construtor completo")
    void constructorCompleto_valoresInformados_devePreservarComponentesDoEvento() {
        // Arrange
        String ipAddress = "192.168.1.10";
        Instant occurredAt = Instant.parse("2026-07-22T12:00:00Z");

        // Act
        UserAuthenticatedEvent event =
                new UserAuthenticatedEvent(
                        null,
                        null,
                        ipAddress,
                        occurredAt
                );

        UserAuthenticatedEvent equivalentEvent =
                new UserAuthenticatedEvent(
                        null,
                        null,
                        ipAddress,
                        occurredAt
                );

        UserAuthenticatedEvent differentEvent =
                new UserAuthenticatedEvent(
                        null,
                        null,
                        "192.168.1.11",
                        occurredAt
                );

        // Assert
        assertAll(
                () -> assertNull(event.user()),
                () -> assertNull(event.client()),
                () -> assertEquals(
                        ipAddress,
                        event.ipAddress()
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
                () -> assertNotEquals(
                        null,
                        event
                ),
                () -> assertNotEquals(
                        "outro tipo",
                        event
                ),
                () -> assertTrue(
                        event.toString().contains(ipAddress)
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar valores nulos no construtor completo")
    void constructorCompleto_valoresNulos_deveCriarEventoComComponentesNulos() {
        // Arrange e Act
        UserAuthenticatedEvent event =
                new UserAuthenticatedEvent(
                        null,
                        null,
                        null,
                        null
                );

        // Assert
        assertAll(
                () -> assertNull(event.user()),
                () -> assertNull(event.client()),
                () -> assertNull(event.ipAddress()),
                () -> assertNull(event.occurredAt())
        );
    }

    @Test
    @DisplayName("Deve definir automaticamente o instante atual ao utilizar o construtor simplificado")
    void constructorSimplificado_ipInformado_deveDefinirInstanteAtual() {
        // Arrange
        String ipAddress = "192.168.1.10";
        Instant beforeCreation = Instant.now();

        // Act
        UserAuthenticatedEvent event =
                new UserAuthenticatedEvent(
                        null,
                        null,
                        ipAddress
                );

        Instant afterCreation = Instant.now();

        // Assert
        assertAll(
                () -> assertNull(event.user()),
                () -> assertNull(event.client()),
                () -> assertEquals(
                        ipAddress,
                        event.ipAddress()
                ),
                () -> assertNotNull(event.occurredAt()),
                () -> assertTrue(
                        !event.occurredAt().isBefore(beforeCreation)
                                && !event.occurredAt().isAfter(afterCreation)
                )
        );
    }
}
