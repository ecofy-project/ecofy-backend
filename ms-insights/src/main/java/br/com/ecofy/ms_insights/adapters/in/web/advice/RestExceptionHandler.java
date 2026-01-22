package br.com.ecofy.ms_insights.adapters.in.web.advice;

import br.com.ecofy.ms_insights.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_insights.core.domain.exception.GoalNotFoundException;
import br.com.ecofy.ms_insights.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_insights.core.domain.exception.InsightNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    // Trata GoalNotFoundException retornando 404 com motivo padronizado para facilitar observabilidade/cliente.
    @ExceptionHandler(GoalNotFoundException.class)
    ResponseEntity<ApiErrorResponse> goalNotFound(GoalNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, Map.of("reason", "GOAL_NOT_FOUND"));
    }

    // Trata InsightNotFoundException retornando 404 com motivo padronizado para facilitar observabilidade/cliente.
    @ExceptionHandler(InsightNotFoundException.class)
    ResponseEntity<ApiErrorResponse> insightNotFound(InsightNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, Map.of("reason", "INSIGHT_NOT_FOUND"));
    }

    // Trata violações de idempotência retornando 409 para indicar conflito/repetição de requisição/operação.
    @ExceptionHandler(IdempotencyViolationException.class)
    ResponseEntity<ApiErrorResponse> idem(IdempotencyViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, Map.of("reason", "IDEMPOTENCY_VIOLATION"));
    }

    // Trata validações de regra de negócio retornando 400 com motivo padronizado para consumo do cliente.
    @ExceptionHandler(BusinessValidationException.class)
    ResponseEntity<ApiErrorResponse> business(BusinessValidationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, Map.of("reason", "BUSINESS_VALIDATION"));
    }

    // Trata falhas de validação Bean Validation (DTO) retornando 400 e detalhando erros por campo.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fields.put(err.getField(), err.getDefaultMessage()));
        details.put("fields", fields);
        return build(HttpStatus.BAD_REQUEST, "Invalid payload", req, details);
    }

    // Trata exceções não mapeadas retornando 500 genérico para evitar vazamento de detalhes internos.
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> generic(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req, Map.of());
    }

    // Monta o corpo padrão de erro (com traceId quando disponível) e retorna ResponseEntity com o status apropriado.
    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest req, Map<String, Object> details) {
        String traceId = req.getHeader("X-Trace-Id");
        if (!StringUtils.hasText(traceId)) traceId = req.getHeader("X-Correlation-Id");

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                traceId,
                details
        );
        return ResponseEntity.status(status).body(body);
    }

}
