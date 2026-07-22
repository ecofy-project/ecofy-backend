package br.com.ecofy.auth.core.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do endereço de e-mail")
class EmailAddressTest {

    @Test
    @DisplayName("Deve criar o endereço de e-mail normalizando espaços e letras maiúsculas")
    void constructor_emailValidoComEspacosEMaiusculas_deveCriarEmailNormalizado() {
        // Arrange
        String value = "  Usuario@Exemplo.COM  ";

        // Act
        EmailAddress emailAddress = new EmailAddress(value);

        // Assert
        assertAll(
                () -> assertEquals(
                        "usuario@exemplo.com",
                        emailAddress.value()
                ),
                () -> assertEquals(
                        "usuario@exemplo.com",
                        emailAddress.toString()
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar um endereço de e-mail válido com subdomínio")
    void constructor_emailComSubdominio_deveCriarEmail() {
        // Arrange
        String value = "usuario@conta.exemplo.com";

        // Act
        EmailAddress emailAddress = new EmailAddress(value);

        // Assert
        assertEquals(
                value,
                emailAddress.value()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um endereço de e-mail nulo")
    void constructor_emailNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new EmailAddress(null)
        );

        assertEquals(
                "email must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um endereço de e-mail vazio")
    void constructor_emailVazio_deveLancarIllegalArgumentException() {
        // Arrange
        String value = "";

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress(value)
        );

        // Assert
        assertEquals(
                "Invalid email address: " + value,
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um endereço de e-mail composto somente por espaços")
    void constructor_emailComEspacos_deveLancarIllegalArgumentException() {
        // Arrange
        String value = "   ";

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress(value)
        );

        // Assert
        assertEquals(
                "Invalid email address: " + value,
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um endereço de e-mail sem arroba")
    void constructor_emailSemArroba_deveLancarIllegalArgumentException() {
        // Arrange
        String value = "usuario.exemplo.com";

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress(value)
        );

        // Assert
        assertEquals(
                "Invalid email address: " + value,
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um endereço de e-mail sem domínio")
    void constructor_emailSemDominio_deveLancarIllegalArgumentException() {
        // Arrange
        String value = "usuario@";

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress(value)
        );

        // Assert
        assertEquals(
                "Invalid email address: " + value,
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um endereço de e-mail sem ponto no domínio")
    void constructor_emailSemPontoNoDominio_deveLancarIllegalArgumentException() {
        // Arrange
        String value = "usuario@exemplo";

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress(value)
        );

        // Assert
        assertEquals(
                "Invalid email address: " + value,
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um endereço de e-mail com espaço interno")
    void constructor_emailComEspacoInterno_deveLancarIllegalArgumentException() {
        // Arrange
        String value = "usuario teste@exemplo.com";

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress(value)
        );

        // Assert
        assertEquals(
                "Invalid email address: " + value,
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve considerar igual a própria instância")
    void equals_mesmaInstancia_deveRetornarTrue() {
        // Arrange
        EmailAddress emailAddress = new EmailAddress(
                "usuario@exemplo.com"
        );

        // Act
        boolean result = emailAddress.equals(emailAddress);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar iguais os endereços que resultam no mesmo valor normalizado")
    void equals_emailsComMesmoValorNormalizado_deveRetornarTrue() {
        // Arrange
        EmailAddress emailAddress = new EmailAddress(
                "usuario@exemplo.com"
        );

        EmailAddress equivalentEmail = new EmailAddress(
                "  USUARIO@EXEMPLO.COM  "
        );

        // Act
        boolean result = emailAddress.equals(equivalentEmail);

        // Assert
        assertAll(
                () -> assertTrue(result),
                () -> assertEquals(
                        emailAddress.hashCode(),
                        equivalentEmail.hashCode()
                )
        );
    }

    @Test
    @DisplayName("Deve considerar diferentes os endereços com valores distintos")
    void equals_emailsDiferentes_deveRetornarFalse() {
        // Arrange
        EmailAddress emailAddress = new EmailAddress(
                "usuario@exemplo.com"
        );

        EmailAddress differentEmail = new EmailAddress(
                "outro@exemplo.com"
        );

        // Act
        boolean result = emailAddress.equals(differentEmail);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar diferente um objeto de outro tipo")
    void equals_objetoDeOutroTipo_deveRetornarFalse() {
        // Arrange
        EmailAddress emailAddress = new EmailAddress(
                "usuario@exemplo.com"
        );

        // Act
        boolean result = emailAddress.equals(
                "usuario@exemplo.com"
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar diferente um objeto nulo")
    void equals_objetoNulo_deveRetornarFalse() {
        // Arrange
        EmailAddress emailAddress = new EmailAddress(
                "usuario@exemplo.com"
        );

        // Act
        boolean result = emailAddress.equals(null);

        // Assert
        assertFalse(result);
    }
}
