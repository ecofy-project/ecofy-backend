package br.com.ecofy.gateway.api_gateway.error;

import java.time.Instant;
import java.util.List;

// Representa o contrato padronizado de erro retornado pelo Gateway.
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String traceId,
        List<ApiErrorDetail> details
) {

    public ApiErrorResponse {
        details = (details == null) ? List.of() : List.copyOf(details);
    }

    // Cria uma resposta com o código e a mensagem segura do erro.
    public static ApiErrorResponse of(
            GatewayErrorCode code,
            String path,
            String traceId
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                code.status().value(),
                code.name(),
                code.defaultMessage(),
                path,
                traceId,
                List.of()
        );
    }
}
