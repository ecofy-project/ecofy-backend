package br.com.ecofy.auth.adapters.in.web.dto.response;

import java.time.Instant;
import java.util.List;

// Padroniza as informações retornadas em respostas de erro da API.
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String traceId,
        List<ErrorDetail> details
) {

    public ApiErrorResponse {
        details = (details == null) ? List.of() : List.copyOf(details);
    }

    // Representa um detalhe controlado associado ao erro.
    public record ErrorDetail(String field, String code, String message) {}

    // Cria uma resposta de erro sem detalhes adicionais.
    public static ApiErrorResponse of(
            int status,
            String errorCode,
            String message,
            String path,
            String traceId
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                errorCode,
                message,
                path,
                traceId,
                List.of()
        );
    }

    // Cria uma resposta de erro com detalhes adicionais.
    public static ApiErrorResponse of(
            int status,
            String errorCode,
            String message,
            String path,
            String traceId,
            List<ErrorDetail> details
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                errorCode,
                message,
                path,
                traceId,
                details
        );
    }
}
