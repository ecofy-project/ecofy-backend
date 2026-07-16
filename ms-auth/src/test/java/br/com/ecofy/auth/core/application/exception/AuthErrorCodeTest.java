package br.com.ecofy.auth.core.application.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthErrorCodeTest {

    @Test
    void shouldReturnExpectedHttpStatusAndCodeForAllAuthErrorCodes() {
        Map<AuthErrorCode, HttpStatus> expectedStatuses = Map.ofEntries(
                Map.entry(AuthErrorCode.EMAIL_ALREADY_REGISTERED, HttpStatus.CONFLICT),
                Map.entry(AuthErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED),
                Map.entry(AuthErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND),
                Map.entry(AuthErrorCode.USER_BLOCKED, HttpStatus.FORBIDDEN),
                Map.entry(AuthErrorCode.USER_LOCKED, HttpStatus.FORBIDDEN),
                Map.entry(AuthErrorCode.EMAIL_NOT_VERIFIED, HttpStatus.FORBIDDEN),

                Map.entry(AuthErrorCode.CLIENT_NOT_FOUND, HttpStatus.NOT_FOUND),
                Map.entry(AuthErrorCode.CLIENT_ALREADY_REGISTERED, HttpStatus.CONFLICT),
                Map.entry(AuthErrorCode.CLIENT_INACTIVE, HttpStatus.FORBIDDEN),
                Map.entry(AuthErrorCode.INVALID_REDIRECT_URI, HttpStatus.BAD_REQUEST),

                Map.entry(AuthErrorCode.CURRENT_USER_NOT_AUTHENTICATED, HttpStatus.UNAUTHORIZED),
                Map.entry(AuthErrorCode.USER_PROFILE_NOT_FOUND, HttpStatus.NOT_FOUND),

                Map.entry(AuthErrorCode.EMAIL_CONFIRMATION_TOKEN_INVALID, HttpStatus.BAD_REQUEST),
                Map.entry(AuthErrorCode.EMAIL_CONFIRMATION_TOKEN_EXPIRED, HttpStatus.BAD_REQUEST),
                Map.entry(AuthErrorCode.EMAIL_ALREADY_CONFIRMED, HttpStatus.CONFLICT),

                Map.entry(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID, HttpStatus.BAD_REQUEST),
                Map.entry(AuthErrorCode.PASSWORD_RESET_TOKEN_EXPIRED, HttpStatus.BAD_REQUEST),
                Map.entry(AuthErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED, HttpStatus.BAD_REQUEST),
                Map.entry(AuthErrorCode.PASSWORD_POLICY_VIOLATION, HttpStatus.BAD_REQUEST),

                Map.entry(AuthErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED),
                Map.entry(AuthErrorCode.INVALID_TOKEN_SIGNATURE, HttpStatus.UNAUTHORIZED),
                Map.entry(AuthErrorCode.TOKEN_REVOKED, HttpStatus.UNAUTHORIZED),
                Map.entry(AuthErrorCode.TOKEN_AUDIENCE_MISMATCH, HttpStatus.UNAUTHORIZED),
                Map.entry(AuthErrorCode.TOKEN_ISSUER_MISMATCH, HttpStatus.UNAUTHORIZED),
                Map.entry(AuthErrorCode.TOKEN_MISSING_SCOPE, HttpStatus.FORBIDDEN),
                Map.entry(AuthErrorCode.TOKEN_NOT_FOUND, HttpStatus.NOT_FOUND),
                Map.entry(AuthErrorCode.TOKEN_ALREADY_REVOKED, HttpStatus.BAD_REQUEST),
                Map.entry(AuthErrorCode.TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION, HttpStatus.BAD_REQUEST),
                Map.entry(AuthErrorCode.TOKEN_OWNER_MISMATCH, HttpStatus.FORBIDDEN),

                Map.entry(AuthErrorCode.JWKS_NOT_AVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
                Map.entry(AuthErrorCode.JWK_NOT_FOUND, HttpStatus.NOT_FOUND),
                Map.entry(AuthErrorCode.JWKS_ROTATION_IN_PROGRESS, HttpStatus.SERVICE_UNAVAILABLE),

                Map.entry(AuthErrorCode.INVALID_REGISTRATION_DATA, HttpStatus.BAD_REQUEST),
                Map.entry(AuthErrorCode.WEAK_PASSWORD, HttpStatus.BAD_REQUEST),
                Map.entry(AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE, HttpStatus.FORBIDDEN)
        );

        assertEquals(AuthErrorCode.values().length, expectedStatuses.size());

        expectedStatuses.forEach((errorCode, expectedStatus) -> {
            assertEquals(expectedStatus, errorCode.getHttpStatus());
            assertEquals(errorCode.name(), errorCode.getCode());
        });
    }

    @Test
    void shouldExposeAllEnumValuesInExpectedOrder() {
        AuthErrorCode[] values = AuthErrorCode.values();

        assertArrayEquals(
                new AuthErrorCode[]{
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

                        AuthErrorCode.INVALID_REGISTRATION_DATA,
                        AuthErrorCode.WEAK_PASSWORD,
                        AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE
                },
                values
        );
    }

    @Test
    void shouldResolveEnumByName() {
        for (AuthErrorCode errorCode : AuthErrorCode.values()) {
            AuthErrorCode resolved = AuthErrorCode.valueOf(errorCode.name());

            assertSame(errorCode, resolved);
        }
    }
}