package br.com.ecofy.gateway.api_gateway.error;

import java.time.Instant;
import java.util.List;

/**
 * Contrato padrão de erro do ecossistema (ECO-04).
 *
 * Serializado de forma estável para o consumidor:
 * <pre>
 * {
 *   "timestamp": "2026-07-16T18:00:00Z",
 *   "status": 504,
 *   "errorCode": "GATEWAY_TIMEOUT",
 *   "message": "...",
 *   "path": "/api/v1/budgets",
 *   "traceId": "0af7651916cd43dd8448eb211c80319c",
 *   "details": []
 * }
 * </pre>
 *
 * Regras de segurança: nunca expõe stack trace, nome de classe interna,
 * host/porta de microsserviço, valores de Authorization, cookies ou secrets.
 * A lista {@code details} nunca é {@code null} e serializa como {@code []}.
 */
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

    /**
     * Cria uma resposta de erro a partir de um código do gateway, usando a
     * mensagem segura padrão do código e uma lista de detalhes vazia.
     */
    public static ApiErrorResponse of(GatewayErrorCode code, String path, String traceId) {
        return new ApiErrorResponse(
                Instant.now(),
                code.status().value(),
                code.name(),
                code.defaultMessage(),
                path,
                traceId,
                List.of());
    }
}
