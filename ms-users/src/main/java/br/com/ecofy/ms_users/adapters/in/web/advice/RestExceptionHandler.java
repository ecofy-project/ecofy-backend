package br.com.ecofy.ms_users.adapters.in.web.advice;

import br.com.ecofy.ms_users.core.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    // Trata UserProfileNotFoundException e retorna 404 com payload de erro padronizado (USER_NOT_FOUND).
    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handle(UserProfileNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(ApiErrorResponse.of("USER_NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    // Trata ConnectionNotFoundException e retorna 404 com payload de erro padronizado (CONNECTION_NOT_FOUND).
    @ExceptionHandler(ConnectionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handle(ConnectionNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(ApiErrorResponse.of("CONNECTION_NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    // Trata IdempotencyViolationException e retorna 409 (conflito) com payload de erro padronizado (IDEMPOTENCY_VIOLATION).
    @ExceptionHandler(IdempotencyViolationException.class)
    public ResponseEntity<ApiErrorResponse> handle(IdempotencyViolationException ex, HttpServletRequest req) {
        return ResponseEntity.status(409).body(ApiErrorResponse.of("IDEMPOTENCY_VIOLATION", ex.getMessage(), req.getRequestURI()));
    }

    // Trata BusinessValidationException e retorna 400 com payload de erro padronizado (BUSINESS_VALIDATION).
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorResponse> handle(BusinessValidationException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("BUSINESS_VALIDATION", ex.getMessage(), req.getRequestURI()));
    }

    // Trata falhas de validação do Spring (bean validation) e retorna 400 com payload de erro padronizado (INVALID_PAYLOAD).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handle(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("INVALID_PAYLOAD", "Payload inválido", req.getRequestURI()));
    }

    // Trata qualquer exceção não mapeada e retorna 500 com payload de erro padronizado (INTERNAL_ERROR).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handle(Exception ex, HttpServletRequest req) {
        return ResponseEntity.internalServerError().body(ApiErrorResponse.of("INTERNAL_ERROR", "Erro interno", req.getRequestURI()));
    }

}
