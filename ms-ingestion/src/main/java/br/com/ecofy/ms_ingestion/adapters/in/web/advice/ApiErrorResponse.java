package br.com.ecofy.ms_ingestion.adapters.in.web.advice;

import br.com.ecofy.ms_ingestion.adapters.in.web.correlation.CorrelationId;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

// Define o contrato seguro de erros expostos pela API.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String traceId,
        List<Detail> details
) {

    // Representa um detalhe geral ou associado a uma linha do arquivo.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detail(
            Integer row,
            String field,
            String code,
            String message
    ) {
        public static Detail ofField(String field, String code, String message) {
            return new Detail(null, field, code, message);
        }

        public static Detail ofRow(int row, String field, String code, String message) {
            return new Detail(row, field, code, message);
        }
    }

    public ApiErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public static ApiErrorResponse of(int status, String errorCode, String message, String path) {
        return of(status, errorCode, message, path, List.of());
    }

    public static ApiErrorResponse of(int status,
                                      String errorCode,
                                      String message,
                                      String path,
                                      List<Detail> details) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                errorCode,
                message,
                path,
                CorrelationId.current(),
                details
        );
    }
}
