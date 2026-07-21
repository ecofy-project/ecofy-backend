package br.com.ecofy.auth.core.application.exception;

import org.springframework.http.HttpStatus;

// Centraliza os códigos de erro e os respectivos status HTTP da autenticação.
public enum AuthErrorCode {

    EMAIL_ALREADY_REGISTERED(
            HttpStatus.CONFLICT,
            "EMAIL_ALREADY_REGISTERED"
    ),
    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS"
    ),
    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER_NOT_FOUND"
    ),
    USER_BLOCKED(
            HttpStatus.FORBIDDEN,
            "USER_BLOCKED"
    ),
    USER_LOCKED(
            HttpStatus.FORBIDDEN,
            "USER_LOCKED"
    ),
    EMAIL_NOT_VERIFIED(
            HttpStatus.FORBIDDEN,
            "EMAIL_NOT_VERIFIED"
    ),

    CLIENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "CLIENT_NOT_FOUND"
    ),
    CLIENT_ALREADY_REGISTERED(
            HttpStatus.CONFLICT,
            "CLIENT_ALREADY_REGISTERED"
    ),
    CLIENT_INACTIVE(
            HttpStatus.FORBIDDEN,
            "CLIENT_INACTIVE"
    ),
    INVALID_REDIRECT_URI(
            HttpStatus.BAD_REQUEST,
            "INVALID_REDIRECT_URI"
    ),

    CURRENT_USER_NOT_AUTHENTICATED(
            HttpStatus.UNAUTHORIZED,
            "CURRENT_USER_NOT_AUTHENTICATED"
    ),
    USER_PROFILE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER_PROFILE_NOT_FOUND"
    ),

    EMAIL_CONFIRMATION_TOKEN_INVALID(
            HttpStatus.BAD_REQUEST,
            "EMAIL_CONFIRMATION_TOKEN_INVALID"
    ),
    EMAIL_CONFIRMATION_TOKEN_EXPIRED(
            HttpStatus.BAD_REQUEST,
            "EMAIL_CONFIRMATION_TOKEN_EXPIRED"
    ),
    EMAIL_ALREADY_CONFIRMED(
            HttpStatus.CONFLICT,
            "EMAIL_ALREADY_CONFIRMED"
    ),

    PASSWORD_RESET_TOKEN_INVALID(
            HttpStatus.BAD_REQUEST,
            "PASSWORD_RESET_TOKEN_INVALID"
    ),
    PASSWORD_RESET_TOKEN_EXPIRED(
            HttpStatus.BAD_REQUEST,
            "PASSWORD_RESET_TOKEN_EXPIRED"
    ),
    PASSWORD_RESET_TOKEN_ALREADY_USED(
            HttpStatus.BAD_REQUEST,
            "PASSWORD_RESET_TOKEN_ALREADY_USED"
    ),
    PASSWORD_POLICY_VIOLATION(
            HttpStatus.BAD_REQUEST,
            "PASSWORD_POLICY_VIOLATION"
    ),

    TOKEN_EXPIRED(
            HttpStatus.UNAUTHORIZED,
            "TOKEN_EXPIRED"
    ),
    INVALID_TOKEN_SIGNATURE(
            HttpStatus.UNAUTHORIZED,
            "INVALID_TOKEN_SIGNATURE"
    ),
    TOKEN_REVOKED(
            HttpStatus.UNAUTHORIZED,
            "TOKEN_REVOKED"
    ),
    TOKEN_AUDIENCE_MISMATCH(
            HttpStatus.UNAUTHORIZED,
            "TOKEN_AUDIENCE_MISMATCH"
    ),
    TOKEN_ISSUER_MISMATCH(
            HttpStatus.UNAUTHORIZED,
            "TOKEN_ISSUER_MISMATCH"
    ),
    TOKEN_MISSING_SCOPE(
            HttpStatus.FORBIDDEN,
            "TOKEN_MISSING_SCOPE"
    ),
    TOKEN_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TOKEN_NOT_FOUND"
    ),
    TOKEN_ALREADY_REVOKED(
            HttpStatus.BAD_REQUEST,
            "TOKEN_ALREADY_REVOKED"
    ),
    TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION(
            HttpStatus.BAD_REQUEST,
            "TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION"
    ),
    TOKEN_OWNER_MISMATCH(
            HttpStatus.FORBIDDEN,
            "TOKEN_OWNER_MISMATCH"
    ),

    JWKS_NOT_AVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "JWKS_NOT_AVAILABLE"
    ),
    JWK_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "JWK_NOT_FOUND"
    ),
    JWKS_ROTATION_IN_PROGRESS(
            HttpStatus.SERVICE_UNAVAILABLE,
            "JWKS_ROTATION_IN_PROGRESS"
    ),

    RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "RATE_LIMIT_EXCEEDED"
    ),
    AUTHENTICATION_TEMPORARILY_BLOCKED(
            HttpStatus.TOO_MANY_REQUESTS,
            "AUTHENTICATION_TEMPORARILY_BLOCKED"
    ),

    INVALID_REGISTRATION_DATA(
            HttpStatus.BAD_REQUEST,
            "INVALID_REGISTRATION_DATA"
    ),
    WEAK_PASSWORD(
            HttpStatus.BAD_REQUEST,
            "WEAK_PASSWORD"
    ),
    CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE(
            HttpStatus.FORBIDDEN,
            "CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE"
    );

    private final HttpStatus httpStatus;
    private final String code;

    AuthErrorCode(HttpStatus httpStatus, String code) {
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }
}
