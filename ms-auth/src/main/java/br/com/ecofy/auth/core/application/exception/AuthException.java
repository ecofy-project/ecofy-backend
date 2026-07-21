package br.com.ecofy.auth.core.application.exception;

// Representa falhas da autenticação com código e detalhes controlados.
public class AuthException extends RuntimeException {

    private final AuthErrorCode errorCode;
    private final String detail;

    public AuthException(
            AuthErrorCode errorCode,
            String message
    ) {
        super(message);
        this.errorCode = errorCode;
        this.detail = null;
    }

    public AuthException(
            AuthErrorCode errorCode,
            String message,
            String detail
    ) {
        super(message);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public AuthException(
            AuthErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
        this.detail = null;
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }
}
