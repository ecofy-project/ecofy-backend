package br.com.ecofy.ms_categorization.adapters.in.web.advice;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Payload padronizado de erro da API do ms-categorization.
 * Campos nulos são omitidos (ex.: fieldErrors só aparece em erros de validação).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(

        String code,

        String message,

        Instant timestamp,

        Integer status,

        String path,

        List<FieldError> fieldErrors

) {
    public record FieldError(String field, String message) {}

    // Construtor compacto preservado para compatibilidade (code, message, timestamp).
    public ApiErrorResponse(String code, String message, Instant timestamp) {
        this(code, message, timestamp, null, null, null);
    }

    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(code, message, Instant.now(), status, path, null);
    }

    public static ApiErrorResponse of(int status, String code, String message, String path, List<FieldError> fieldErrors) {
        return new ApiErrorResponse(code, message, Instant.now(), status, path, fieldErrors);
    }
}
