package br.com.ecofy.ms_categorization.adapters.in.web.advice;

import br.com.ecofy.ms_categorization.adapters.correlation.CorrelationContext;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

// Define o contrato transversal de erro exposto pela API, sem expor detalhes internos.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(

        Instant timestamp,

        Integer status,

        String errorCode,

        String message,

        String path,

        String traceId,

        List<Detail> details

) {

    // Representa um detalhe de erro, com o campo preenchido em validações por campo.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detail(String field, String code, String message) {
        public static Detail ofField(String field, String code, String message) {
            return new Detail(field, code, message);
        }
    }

    // Representa um detalhe de campo no vocabulário legado usado pelo handler de validação.
    public record FieldError(String field, String message) {
    }

    // Normaliza details para lista vazia, garantindo que o contrato nunca retorne null.
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
                CorrelationContext.currentCorrelationId(),
                details
        );
    }

    // Expõe alias de errorCode mantido por compatibilidade de compilação.
    @Deprecated
    public String code() {
        return errorCode;
    }
}
