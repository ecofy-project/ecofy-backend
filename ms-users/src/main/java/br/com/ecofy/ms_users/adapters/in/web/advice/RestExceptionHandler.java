package br.com.ecofy.ms_users.adapters.in.web.advice;

import br.com.ecofy.ms_users.core.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handle(UserProfileNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(ApiErrorResponse.of("USER_NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ConnectionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handle(ConnectionNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(ApiErrorResponse.of("CONNECTION_NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(IdempotencyViolationException.class)
    public ResponseEntity<ApiErrorResponse> handle(IdempotencyViolationException ex, HttpServletRequest req) {
        return ResponseEntity.status(409).body(ApiErrorResponse.of("IDEMPOTENCY_VIOLATION", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorResponse> handle(BusinessValidationException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("BUSINESS_VALIDATION", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handle(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("INVALID_PAYLOAD", "Payload inválido", req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handle(Exception ex, HttpServletRequest req) {
        return ResponseEntity.internalServerError().body(ApiErrorResponse.of("INTERNAL_ERROR", "Erro interno", req.getRequestURI()));
    }
}
