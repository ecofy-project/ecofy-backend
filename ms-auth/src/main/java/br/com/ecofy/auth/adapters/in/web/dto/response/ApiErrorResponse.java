package br.com.ecofy.auth.adapters.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Payload padronizado de erro retornado por toda a API do ms-auth.
 * Campos nulos/ausentes são omitidos (ex.: fieldErrors só aparece em erros de validação).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> fieldErrors
) {
    public record FieldValidationError(String field, String message) {}

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ApiErrorResponse of(
            int status,
            String error,
            String message,
            String path,
            List<FieldValidationError> fieldErrors
    ) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
