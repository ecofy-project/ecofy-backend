package br.com.ecofy.ms_users.adapters.in.web.advice;

import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;

// Define o contrato padrão de erro do ecossistema, expondo apenas dados seguros ao cliente.
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String traceId,
        List<ErrorDetail> details
) {

    // Aponta a chave de MDC preenchida pelo filtro de correlação.
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    public ApiErrorResponse {
        details = (details == null) ? List.of() : List.copyOf(details);
    }

    // Descreve um detalhe controlado do erro, como uma violação de validação por campo.
    public record ErrorDetail(String field, String code, String message) {}

    public static ApiErrorResponse of(int status, String errorCode, String message, String path) {
        return of(status, errorCode, message, path, List.of());
    }

    public static ApiErrorResponse of(
            int status,
            String errorCode,
            String message,
            String path,
            List<ErrorDetail> details
    ) {
        return new ApiErrorResponse(
                Instant.now(), status, errorCode, message, path, currentTraceId(), details);
    }

    // Recupera o correlation ID da requisição corrente a partir do MDC.
    private static String currentTraceId() {
        return MDC.get(CORRELATION_ID_MDC_KEY);
    }
}
