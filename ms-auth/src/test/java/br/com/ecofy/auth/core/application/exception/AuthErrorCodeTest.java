package br.com.ecofy.auth.core.application.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Testes unitários dos códigos de erro de autenticação")
class AuthErrorCodeTest {

    @Test
    @DisplayName("Deve retornar todos os códigos de erro na ordem declarada")
    void values_codigosDeclarados_deveRetornarTodosNaOrdemCorreta() {
        // Arrange
        AuthErrorCode[] expected = {
                AuthErrorCode.EMAIL_ALREADY_REGISTERED,
                AuthErrorCode.INVALID_CREDENTIALS,
                AuthErrorCode.USER_NOT_FOUND,
                AuthErrorCode.USER_BLOCKED,
                AuthErrorCode.USER_LOCKED,
                AuthErrorCode.EMAIL_NOT_VERIFIED,
                AuthErrorCode.CLIENT_NOT_FOUND,
                AuthErrorCode.CLIENT_ALREADY_REGISTERED,
                AuthErrorCode.CLIENT_INACTIVE,
                AuthErrorCode.INVALID_REDIRECT_URI,
                AuthErrorCode.CURRENT_USER_NOT_AUTHENTICATED,
                AuthErrorCode.USER_PROFILE_NOT_FOUND,
                AuthErrorCode.EMAIL_CONFIRMATION_TOKEN_INVALID,
                AuthErrorCode.EMAIL_CONFIRMATION_TOKEN_EXPIRED,
                AuthErrorCode.EMAIL_ALREADY_CONFIRMED,
                AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                AuthErrorCode.PASSWORD_RESET_TOKEN_EXPIRED,
                AuthErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED,
                AuthErrorCode.PASSWORD_POLICY_VIOLATION,
                AuthErrorCode.TOKEN_EXPIRED,
                AuthErrorCode.INVALID_TOKEN_SIGNATURE,
                AuthErrorCode.TOKEN_REVOKED,
                AuthErrorCode.TOKEN_AUDIENCE_MISMATCH,
                AuthErrorCode.TOKEN_ISSUER_MISMATCH,
                AuthErrorCode.TOKEN_MISSING_SCOPE,
                AuthErrorCode.TOKEN_NOT_FOUND,
                AuthErrorCode.TOKEN_ALREADY_REVOKED,
                AuthErrorCode.TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION,
                AuthErrorCode.TOKEN_OWNER_MISMATCH,
                AuthErrorCode.JWKS_NOT_AVAILABLE,
                AuthErrorCode.JWK_NOT_FOUND,
                AuthErrorCode.JWKS_ROTATION_IN_PROGRESS,
                AuthErrorCode.RATE_LIMIT_EXCEEDED,
                AuthErrorCode.AUTHENTICATION_TEMPORARILY_BLOCKED,
                AuthErrorCode.INVALID_REGISTRATION_DATA,
                AuthErrorCode.WEAK_PASSWORD,
                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE
        };

        // Act
        AuthErrorCode[] result = AuthErrorCode.values();

        // Assert
        assertArrayEquals(expected, result);
    }

    @Test
    @DisplayName("Deve retornar o código textual e o status HTTP configurados para cada erro")
    void getters_todosOsCodigos_deveRetornarCodigoEStatusHttpConfigurados() {
        // Arrange
        Map<AuthErrorCode, HttpStatus> expectedStatuses = Map.ofEntries(
                Map.entry(
                        AuthErrorCode.EMAIL_ALREADY_REGISTERED,
                        HttpStatus.CONFLICT
                ),
                Map.entry(
                        AuthErrorCode.INVALID_CREDENTIALS,
                        HttpStatus.UNAUTHORIZED
                ),
                Map.entry(
                        AuthErrorCode.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ),
                Map.entry(
                        AuthErrorCode.USER_BLOCKED,
                        HttpStatus.FORBIDDEN
                ),
                Map.entry(
                        AuthErrorCode.USER_LOCKED,
                        HttpStatus.FORBIDDEN
                ),
                Map.entry(
                        AuthErrorCode.EMAIL_NOT_VERIFIED,
                        HttpStatus.FORBIDDEN
                ),
                Map.entry(
                        AuthErrorCode.CLIENT_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ),
                Map.entry(
                        AuthErrorCode.CLIENT_ALREADY_REGISTERED,
                        HttpStatus.CONFLICT
                ),
                Map.entry(
                        AuthErrorCode.CLIENT_INACTIVE,
                        HttpStatus.FORBIDDEN
                ),
                Map.entry(
                        AuthErrorCode.INVALID_REDIRECT_URI,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.CURRENT_USER_NOT_AUTHENTICATED,
                        HttpStatus.UNAUTHORIZED
                ),
                Map.entry(
                        AuthErrorCode.USER_PROFILE_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ),
                Map.entry(
                        AuthErrorCode.EMAIL_CONFIRMATION_TOKEN_INVALID,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.EMAIL_CONFIRMATION_TOKEN_EXPIRED,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.EMAIL_ALREADY_CONFIRMED,
                        HttpStatus.CONFLICT
                ),
                Map.entry(
                        AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.PASSWORD_RESET_TOKEN_EXPIRED,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.PASSWORD_POLICY_VIOLATION,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.TOKEN_EXPIRED,
                        HttpStatus.UNAUTHORIZED
                ),
                Map.entry(
                        AuthErrorCode.INVALID_TOKEN_SIGNATURE,
                        HttpStatus.UNAUTHORIZED
                ),
                Map.entry(
                        AuthErrorCode.TOKEN_REVOKED,
                        HttpStatus.UNAUTHORIZED
                ),
                Map.entry(
                        AuthErrorCode.TOKEN_AUDIENCE_MISMATCH,
                        HttpStatus.UNAUTHORIZED
                ),
                Map.entry(
                        AuthErrorCode.TOKEN_ISSUER_MISMATCH,
                        HttpStatus.UNAUTHORIZED
                ),
                Map.entry(
                        AuthErrorCode.TOKEN_MISSING_SCOPE,
                        HttpStatus.FORBIDDEN
                ),
                Map.entry(
                        AuthErrorCode.TOKEN_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ),
                Map.entry(
                        AuthErrorCode.TOKEN_ALREADY_REVOKED,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.TOKEN_OWNER_MISMATCH,
                        HttpStatus.FORBIDDEN
                ),
                Map.entry(
                        AuthErrorCode.JWKS_NOT_AVAILABLE,
                        HttpStatus.SERVICE_UNAVAILABLE
                ),
                Map.entry(
                        AuthErrorCode.JWK_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ),
                Map.entry(
                        AuthErrorCode.JWKS_ROTATION_IN_PROGRESS,
                        HttpStatus.SERVICE_UNAVAILABLE
                ),
                Map.entry(
                        AuthErrorCode.RATE_LIMIT_EXCEEDED,
                        HttpStatus.TOO_MANY_REQUESTS
                ),
                Map.entry(
                        AuthErrorCode.AUTHENTICATION_TEMPORARILY_BLOCKED,
                        HttpStatus.TOO_MANY_REQUESTS
                ),
                Map.entry(
                        AuthErrorCode.INVALID_REGISTRATION_DATA,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.WEAK_PASSWORD,
                        HttpStatus.BAD_REQUEST
                ),
                Map.entry(
                        AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                        HttpStatus.FORBIDDEN
                )
        );

        // Act
        AuthErrorCode[] errorCodes = AuthErrorCode.values();

        // Assert
        assertEquals(errorCodes.length, expectedStatuses.size());

        for (AuthErrorCode errorCode : errorCodes) {
            assertAll(
                    () -> assertEquals(
                            expectedStatuses.get(errorCode),
                            errorCode.getHttpStatus()
                    ),
                    () -> assertEquals(
                            errorCode.name(),
                            errorCode.getCode()
                    )
            );
        }
    }

    @Test
    @DisplayName("Deve retornar a constante correspondente para cada nome válido")
    void valueOf_nomesValidos_deveRetornarConstantesCorrespondentes() {
        // Arrange
        AuthErrorCode[] errorCodes = AuthErrorCode.values();

        // Act e Assert
        for (AuthErrorCode expected : errorCodes) {
            AuthErrorCode result = AuthErrorCode.valueOf(expected.name());

            assertSame(expected, result);
        }
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome do código não existir")
    void valueOf_nomeInexistente_deveLancarIllegalArgumentException() {
        // Arrange
        String invalidName = "AUTH_ERROR_INEXISTENTE";

        // Act e Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> AuthErrorCode.valueOf(invalidName)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome do código for nulo")
    void valueOf_nomeNulo_deveLancarNullPointerException() {
        // Arrange
        String nullName = null;

        // Act e Assert
        assertThrows(
                NullPointerException.class,
                () -> AuthErrorCode.valueOf(nullName)
        );
    }
}
