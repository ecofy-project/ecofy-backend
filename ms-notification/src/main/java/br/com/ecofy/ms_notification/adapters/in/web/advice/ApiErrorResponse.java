package br.com.ecofy.ms_notification.adapters.in.web.advice;

import org.slf4j.MDC;

import java.time.Instant;

// Padroniza os dados retornados nas respostas de erro da API.
public record ApiErrorResponse(
        String errorCode,
        String message,
        Instant timestamp,
        String path,
        String traceId
) {

    // Cria uma resposta de erro com o identificador de rastreamento atual.
    public static ApiErrorResponse of(String errorCode, String message, String path) {
        return new ApiErrorResponse(
                errorCode,
                message,
                Instant.now(),
                path,
                currentTraceId()
        );
    }

    // Cria uma resposta de erro com o identificador de rastreamento informado.
    public static ApiErrorResponse of(
            String errorCode,
            String message,
            String path,
            String traceId
    ) {
        return new ApiErrorResponse(
                errorCode,
                message,
                Instant.now(),
                path,
                traceId
        );
    }

    // Resolve o identificador de rastreamento disponível no contexto atual.
    private static String currentTraceId() {
        String correlationId = MDC.get("correlationId");
        return (correlationId == null || correlationId.isBlank())
                ? null
                : correlationId;
    }
}
