package br.com.ecofy.ms_notification.adapters.in.web.advice;

import org.slf4j.MDC;

import java.time.Instant;

public record ApiErrorResponse(

        String errorCode,

        String message,

        Instant timestamp,

        String path,

        // Correção Dia 7 (item #8/#13): correlationId/traceId para o cliente correlacionar com os logs
        // do servidor sem que precisemos vazar detalhes internos da exceção.
        String traceId

) {

    public static ApiErrorResponse of(String errorCode, String message, String path) {
        return new ApiErrorResponse(errorCode, message, Instant.now(), path, currentTraceId());
    }

    public static ApiErrorResponse of(String errorCode, String message, String path, String traceId) {
        return new ApiErrorResponse(errorCode, message, Instant.now(), path, traceId);
    }

    // Lê o correlationId do MDC (preenchido pelo CorrelationIdFilter); retorna null se ausente.
    private static String currentTraceId() {
        String correlationId = MDC.get("correlationId");
        return (correlationId == null || correlationId.isBlank()) ? null : correlationId;
    }
}
