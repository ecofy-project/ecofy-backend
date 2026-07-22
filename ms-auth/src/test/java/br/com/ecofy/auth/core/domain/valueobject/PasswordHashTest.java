package br.com.ecofy.auth.core.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do hash de senha")
class PasswordHashTest {

    @Test
    @DisplayName("Deve criar o hash de senha e preservar o valor informado")
    void constructor_valorValido_deveCriarPasswordHash() {
        // Arrange
        String value = "$2a$12$hashSeguro";

        // Act
        PasswordHash passwordHash = new PasswordHash(value);

        // Assert
        assertEquals(value, passwordHash.value());
    }

    @Test
    @DisplayName("Deve aceitar uma string vazia como valor do hash")
    void constructor_valorVazio_deveCriarPasswordHash() {
        // Arrange
        String value = "";

        // Act
        PasswordHash passwordHash = new PasswordHash(value);

        // Assert
        assertEquals(value, passwordHash.value());
    }

    @Test
    @DisplayName("Deve rejeitar um valor nulo para o hash de senha")
    void constructor_valorNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new PasswordHash(null)
        );

        assertEquals(
                "password hash must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve ocultar o conteúdo do hash na representação textual")
    void toString_hashExistente_deveRetornarValorMascarado() {
        // Arrange
        PasswordHash passwordHash = new PasswordHash(
                "$2a$12$hashSeguro"
        );

        // Act
        String result = passwordHash.toString();

        // Assert
        assertEquals("********", result);
    }

    @Test
    @DisplayName("Deve considerar igual a própria instância")
    void equals_mesmaInstancia_deveRetornarTrue() {
        // Arrange
        PasswordHash passwordHash = new PasswordHash(
                "$2a$12$hashSeguro"
        );

        // Act
        boolean result = passwordHash.equals(passwordHash);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar iguais os hashes com o mesmo valor")
    void equals_hashesComMesmoValor_deveRetornarTrue() {
        // Arrange
        PasswordHash passwordHash = new PasswordHash(
                "$2a$12$hashSeguro"
        );
        PasswordHash equivalentHash = new PasswordHash(
                "$2a$12$hashSeguro"
        );

        // Act
        boolean result = passwordHash.equals(equivalentHash);

        // Assert
        assertAll(
                () -> assertTrue(result),
                () -> assertEquals(
                        passwordHash.hashCode(),
                        equivalentHash.hashCode()
                )
        );
    }

    @Test
    @DisplayName("Deve considerar diferentes os hashes com valores distintos")
    void equals_hashesComValoresDiferentes_deveRetornarFalse() {
        // Arrange
        PasswordHash passwordHash = new PasswordHash(
                "$2a$12$hashSeguro"
        );
        PasswordHash differentHash = new PasswordHash(
                "$2a$12$outroHash"
        );

        // Act
        boolean result = passwordHash.equals(differentHash);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar diferente um objeto de outro tipo")
    void equals_objetoDeOutroTipo_deveRetornarFalse() {
        // Arrange
        PasswordHash passwordHash = new PasswordHash(
                "$2a$12$hashSeguro"
        );

        // Act
        boolean result = passwordHash.equals(
                "$2a$12$hashSeguro"
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar diferente um objeto nulo")
    void equals_objetoNulo_deveRetornarFalse() {
        // Arrange
        PasswordHash passwordHash = new PasswordHash(
                "$2a$12$hashSeguro"
        );

        // Act
        boolean result = passwordHash.equals(null);

        // Assert
        assertFalse(result);
    }
}
