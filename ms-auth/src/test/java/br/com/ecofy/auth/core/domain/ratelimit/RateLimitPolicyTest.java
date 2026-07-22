package br.com.ecofy.auth.core.domain.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários da política de limite de requisições")
class RateLimitPolicyTest {

    @Test
    @DisplayName("Deve criar a política quando todos os dados forem válidos")
    void constructor_dadosValidos_deveCriarPolitica() {
        // Arrange
        String name = "login";
        int limit = 5;
        Duration window = Duration.ofMinutes(1);

        // Act
        RateLimitPolicy policy = new RateLimitPolicy(
                name,
                limit,
                window
        );

        RateLimitPolicy equivalentPolicy = new RateLimitPolicy(
                name,
                limit,
                window
        );

        RateLimitPolicy differentPolicy = new RateLimitPolicy(
                "refresh-token",
                limit,
                window
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        name,
                        policy.name()
                ),
                () -> assertEquals(
                        limit,
                        policy.limit()
                ),
                () -> assertEquals(
                        window,
                        policy.window()
                ),
                () -> assertEquals(
                        equivalentPolicy,
                        policy
                ),
                () -> assertEquals(
                        equivalentPolicy.hashCode(),
                        policy.hashCode()
                ),
                () -> assertNotEquals(
                        differentPolicy,
                        policy
                ),
                () -> assertNotEquals(
                        null,
                        policy
                ),
                () -> assertNotEquals(
                        "outro tipo",
                        policy
                ),
                () -> assertTrue(
                        policy.toString().contains(name)
                ),
                () -> assertTrue(
                        policy.toString().contains(
                                String.valueOf(limit)
                        )
                ),
                () -> assertTrue(
                        policy.toString().contains(
                                window.toString()
                        )
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar os menores valores positivos para limite e janela")
    void constructor_valoresMinimosPositivos_deveCriarPolitica() {
        // Arrange
        int limit = 1;
        Duration window = Duration.ofNanos(1);

        // Act
        RateLimitPolicy policy = new RateLimitPolicy(
                "minimum-policy",
                limit,
                window
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        limit,
                        policy.limit()
                ),
                () -> assertEquals(
                        window,
                        policy.window()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar o nome nulo")
    void constructor_nomeNulo_deveLancarNullPointerException() {
        // Arrange
        Duration window = Duration.ofMinutes(1);

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RateLimitPolicy(
                        null,
                        5,
                        window
                )
        );

        // Assert
        assertEquals(
                "name must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar a janela nula")
    void constructor_janelaNula_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RateLimitPolicy(
                        "login",
                        5,
                        null
                )
        );

        assertEquals(
                "window must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o nome vazio")
    void constructor_nomeVazio_deveLancarIllegalArgumentException() {
        // Arrange
        Duration window = Duration.ofMinutes(1);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RateLimitPolicy(
                        "",
                        5,
                        window
                )
        );

        // Assert
        assertEquals(
                "name must not be blank",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o nome composto somente por espaços")
    void constructor_nomeComEspacos_deveLancarIllegalArgumentException() {
        // Arrange
        Duration window = Duration.ofMinutes(1);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RateLimitPolicy(
                        "   ",
                        5,
                        window
                )
        );

        // Assert
        assertEquals(
                "name must not be blank",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o limite igual a zero")
    void constructor_limiteZero_deveLancarIllegalArgumentException() {
        // Arrange
        Duration window = Duration.ofMinutes(1);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RateLimitPolicy(
                        "login",
                        0,
                        window
                )
        );

        // Assert
        assertEquals(
                "limit must be > 0",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o limite negativo")
    void constructor_limiteNegativo_deveLancarIllegalArgumentException() {
        // Arrange
        Duration window = Duration.ofMinutes(1);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RateLimitPolicy(
                        "login",
                        -1,
                        window
                )
        );

        // Assert
        assertEquals(
                "limit must be > 0",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar a janela com duração igual a zero")
    void constructor_janelaZero_deveLancarIllegalArgumentException() {
        // Arrange, Act e Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RateLimitPolicy(
                        "login",
                        5,
                        Duration.ZERO
                )
        );

        assertEquals(
                "window must be > 0",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar a janela com duração negativa")
    void constructor_janelaNegativa_deveLancarIllegalArgumentException() {
        // Arrange
        Duration window = Duration.ofNanos(-1);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new RateLimitPolicy(
                        "login",
                        5,
                        window
                )
        );

        // Assert
        assertEquals(
                "window must be > 0",
                exception.getMessage()
        );
    }
}
