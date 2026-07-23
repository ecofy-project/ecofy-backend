package br.com.ecofy.ms_insights.adapters.in.web.advice;

import br.com.ecofy.ms_insights.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_insights.core.domain.exception.ExternalDataUnavailableException;
import br.com.ecofy.ms_insights.core.domain.exception.GoalNotFoundException;
import br.com.ecofy.ms_insights.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_insights.core.domain.exception.InsightNotFoundException;
import br.com.ecofy.ms_insights.core.domain.exception.RebuildRunNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Centraliza o tratamento de erros expostos pela API de insights.
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    // Converte a ausência de uma meta em resposta HTTP 404.
    @ExceptionHandler(GoalNotFoundException.class)
    ResponseEntity<ApiErrorResponse> goalNotFound(GoalNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, Map.of("reason", "GOAL_NOT_FOUND"));
    }

    // Converte a ausência de um insight em resposta HTTP 404.
    @ExceptionHandler(InsightNotFoundException.class)
    ResponseEntity<ApiErrorResponse> insightNotFound(InsightNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, Map.of("reason", "INSIGHT_NOT_FOUND"));
    }

    // Converte a ausência de uma execução de rebuild em resposta HTTP 404.
    @ExceptionHandler(RebuildRunNotFoundException.class)
    ResponseEntity<ApiErrorResponse> rebuildNotFound(RebuildRunNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, Map.of("reason", "REBUILD_RUN_NOT_FOUND"));
    }

    // Converte violações de idempotência em resposta HTTP 409.
    @ExceptionHandler(IdempotencyViolationException.class)
    ResponseEntity<ApiErrorResponse> idem(IdempotencyViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, Map.of("reason", "IDEMPOTENCY_VIOLATION"));
    }

    // Converte violações de negócio em resposta HTTP 400.
    @ExceptionHandler(BusinessValidationException.class)
    ResponseEntity<ApiErrorResponse> business(BusinessValidationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, Map.of("reason", "BUSINESS_VALIDATION"));
    }

    // Detalha violações de campos em uma resposta HTTP 400.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fields.put(err.getField(), err.getDefaultMessage()));
        details.put("fields", fields);
        return build(HttpStatus.BAD_REQUEST, "Invalid payload", req, details);
    }

    // Converte violações de parâmetros em resposta HTTP 400.
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> constraint(ConstraintViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, Map.of("reason", "CONSTRAINT_VIOLATION"));
    }

    // Converte corpos ilegíveis em resposta HTTP 400.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", req, Map.of("reason", "MALFORMED_REQUEST"));
    }

    // Converte argumentos inválidos em resposta HTTP 400.
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, Map.of("reason", "INVALID_ARGUMENT"));
    }

    // Converte indisponibilidade externa em resposta HTTP 503.
    @ExceptionHandler(ExternalDataUnavailableException.class)
    ResponseEntity<ApiErrorResponse> externalUnavailable(ExternalDataUnavailableException ex, HttpServletRequest req) {
        log.warn("[RestExceptionHandler] external data unavailable source={} message={}", ex.getSource(), ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "External dependency unavailable: " + ex.getSource(), req,
                Map.of("reason", "EXTERNAL_DATA_UNAVAILABLE", "source", ex.getSource()));
    }

    // Converte falhas inesperadas em uma resposta genérica HTTP 500.
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> generic(Exception ex, HttpServletRequest req) {
        log.error("[RestExceptionHandler] unexpected error path={} type={} message={}",
                req.getRequestURI(), ex.getClass().getName(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno ao processar a solicitação.",
                req, Map.of("reason", "INTERNAL_SERVER_ERROR"));
    }

    // Centraliza a construção das respostas de erro.
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
