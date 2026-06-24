package br.com.ecofy.auth.core.application.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthExceptionTest {

    @Test
    void shouldCreateAuthExceptionWithErrorCodeAndMessage() {
        AuthErrorCode errorCode = AuthErrorCode.INVALID_CREDENTIALS;
        String message = "Credenciais inválidas";

        AuthException exception = new AuthException(errorCode, message);

        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getDetail());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateAuthExceptionWithErrorCodeMessageAndDetail() {
        AuthErrorCode errorCode = AuthErrorCode.EMAIL_ALREADY_REGISTERED;
        String message = "E-mail já cadastrado";
        String detail = "O e-mail informado já existe na base de usuários";

        AuthException exception = new AuthException(errorCode, message, detail);

        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
        assertEquals(detail, exception.getDetail());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateAuthExceptionWithErrorCodeMessageAndCause() {
        AuthErrorCode errorCode = AuthErrorCode.TOKEN_EXPIRED;
        String message = "Token expirado";
        Throwable cause = new IllegalArgumentException("JWT expirado");

        AuthException exception = new AuthException(errorCode, message, cause);

        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getDetail());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldBeInstanceOfRuntimeException() {
        AuthException exception = new AuthException(
                AuthErrorCode.USER_NOT_FOUND,
                "Usuário não encontrado"
        );

        assertInstanceOf(RuntimeException.class, exception);
    }
}