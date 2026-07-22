package br.com.ecofy.auth.core.application.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("Testes unitários da exceção de autenticação")
class AuthExceptionTest {

    @Test
    @DisplayName("Deve criar a exceção com código e mensagem, mantendo detalhe e causa nulos")
    void constructor_codigoEMensagem_deveCriarExcecaoSemDetalheECausa() {
        // Arrange
        AuthErrorCode errorCode =
                AuthErrorCode.INVALID_CREDENTIALS;

        String message = "Credenciais inválidas";

        // Act
        AuthException exception = new AuthException(
                errorCode,
                message
        );

        // Assert
        assertAll(
                () -> assertSame(
                        errorCode,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        message,
                        exception.getMessage()
                ),
                () -> assertNull(exception.getDetail()),
                () -> assertNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve criar a exceção com código, mensagem e detalhe informados")
    void constructor_codigoMensagemEDetalhe_deveCriarExcecaoComDetalhe() {
        // Arrange
        AuthErrorCode errorCode =
                AuthErrorCode.PASSWORD_POLICY_VIOLATION;

        String message = "A senha não atende à política definida";
        String detail = "A senha deve possuir ao menos um número";

        // Act
        AuthException exception = new AuthException(
                errorCode,
                message,
                detail
        );

        // Assert
        assertAll(
                () -> assertSame(
                        errorCode,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        message,
                        exception.getMessage()
                ),
                () -> assertEquals(
                        detail,
                        exception.getDetail()
                ),
                () -> assertNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve criar a exceção com código, mensagem e causa, mantendo o detalhe nulo")
    void constructor_codigoMensagemECausa_deveCriarExcecaoComCausa() {
        // Arrange
        AuthErrorCode errorCode =
                AuthErrorCode.JWKS_NOT_AVAILABLE;

        String message = "Não foi possível acessar as chaves públicas";
        Throwable cause =
                new IllegalStateException("Serviço indisponível");

        // Act
        AuthException exception = new AuthException(
                errorCode,
                message,
                cause
        );

        // Assert
        assertAll(
                () -> assertSame(
                        errorCode,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        message,
                        exception.getMessage()
                ),
                () -> assertSame(
                        cause,
                        exception.getCause()
                ),
                () -> assertNull(exception.getDetail())
        );
    }

    @Test
    @DisplayName("Deve aceitar valores nulos quando nenhuma validação for definida pela classe")
    void constructor_valoresNulos_deveManterValoresNulos() {
        // Arrange
        AuthErrorCode errorCode = null;
        String message = null;
        String detail = null;

        // Act
        AuthException exception = new AuthException(
                errorCode,
                message,
                detail
        );

        // Assert
        assertAll(
                () -> assertNull(exception.getErrorCode()),
                () -> assertNull(exception.getMessage()),
                () -> assertNull(exception.getDetail()),
                () -> assertNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve aceitar mensagem e detalhe vazios quando nenhuma validação for definida pela classe")
    void constructor_mensagemEDetalheVazios_deveManterValoresVazios() {
        // Arrange
        AuthErrorCode errorCode =
                AuthErrorCode.INVALID_REGISTRATION_DATA;

        String message = "";
        String detail = "";

        // Act
        AuthException exception = new AuthException(
                errorCode,
                message,
                detail
        );

        // Assert
        assertAll(
                () -> assertSame(
                        errorCode,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        message,
                        exception.getMessage()
                ),
                () -> assertEquals(
                        detail,
                        exception.getDetail()
                )
        );
    }
}
