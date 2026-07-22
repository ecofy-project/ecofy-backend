package br.com.ecofy.auth.core.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do identificador do usuário de autenticação")
class AuthUserIdTest {

    @Test
    @DisplayName("Deve criar o identificador e preservar o UUID informado")
    void constructor_uuidValido_deveCriarIdentificador() {
        // Arrange
        UUID value = UUID.fromString(
                "7f0b5416-5322-4c34-873e-b96aa3f62639"
        );

        // Act
        AuthUserId authUserId = new AuthUserId(value);

        // Assert
        assertAll(
                () -> assertSame(
                        value,
                        authUserId.value()
                ),
                () -> assertEquals(
                        value.toString(),
                        authUserId.toString()
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar um UUID com todos os bits iguais a zero")
    void constructor_uuidComBitsZerados_deveCriarIdentificador() {
        // Arrange
        UUID value = new UUID(0L, 0L);

        // Act
        AuthUserId authUserId = new AuthUserId(value);

        // Assert
        assertEquals(
                value,
                authUserId.value()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um UUID nulo")
    void constructor_uuidNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUserId(null)
        );

        assertEquals(
                "value must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve gerar um novo identificador com UUID não nulo")
    void newId_semParametros_deveGerarNovoIdentificador() {
        // Arrange e Act
        AuthUserId firstId = AuthUserId.newId();
        AuthUserId secondId = AuthUserId.newId();

        // Assert
        assertAll(
                () -> assertNotNull(firstId),
                () -> assertNotNull(firstId.value()),
                () -> assertNotNull(secondId.value()),
                () -> assertNotEquals(
                        firstId,
                        secondId
                )
        );
    }

    @Test
    @DisplayName("Deve considerar iguais os identificadores com o mesmo UUID")
    void equals_mesmoUuid_deveRetornarTrue() {
        // Arrange
        UUID value = UUID.fromString(
                "7f0b5416-5322-4c34-873e-b96aa3f62639"
        );

        AuthUserId authUserId = new AuthUserId(value);
        AuthUserId equivalentId = new AuthUserId(value);

        // Act e Assert
        assertAll(
                () -> assertTrue(
                        authUserId.equals(authUserId)
                ),
                () -> assertTrue(
                        authUserId.equals(equivalentId)
                ),
                () -> assertEquals(
                        authUserId,
                        equivalentId
                ),
                () -> assertEquals(
                        authUserId.hashCode(),
                        equivalentId.hashCode()
                )
        );
    }

    @Test
    @DisplayName("Deve considerar diferentes os identificadores com UUIDs distintos")
    void equals_uuidsDiferentes_deveRetornarFalse() {
        // Arrange
        AuthUserId authUserId = new AuthUserId(
                UUID.fromString(
                        "7f0b5416-5322-4c34-873e-b96aa3f62639"
                )
        );

        AuthUserId differentId = new AuthUserId(
                UUID.fromString(
                        "084fc53e-0845-461f-968f-b0f707ab8156"
                )
        );

        // Act e Assert
        assertFalse(
                authUserId.equals(differentId)
        );
    }

    @Test
    @DisplayName("Deve considerar diferente um objeto de outro tipo")
    void equals_objetoDeOutroTipo_deveRetornarFalse() {
        // Arrange
        AuthUserId authUserId = AuthUserId.newId();

        // Act e Assert
        assertFalse(
                authUserId.equals("outro tipo")
        );
    }

    @Test
    @DisplayName("Deve considerar diferente um objeto nulo")
    void equals_objetoNulo_deveRetornarFalse() {
        // Arrange
        AuthUserId authUserId = AuthUserId.newId();

        // Act e Assert
        assertFalse(
                authUserId.equals(null)
        );
    }
}
