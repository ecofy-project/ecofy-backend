package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.response.ApiErrorResponse;
import br.com.ecofy.auth.core.application.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Handler global de erros. Traduz exceções de domínio/aplicação e de validação
 * para respostas HTTP consistentes ({@link ApiErrorResponse}), sem vazar stack
 * traces nem dados sensíveis ao cliente.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Exceções de domínio/aplicação: usam o HttpStatus e o code definidos no AuthErrorCode.
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(AuthException ex, HttpServletRequest request) {
        HttpStatus status = ex.getErrorCode().getHttpStatus();

        log.warn(
                "[GlobalExceptionHandler] - [handleAuthException] -> code={} status={} path={}",
                ex.getErrorCode().getCode(),
                status.value(),
                request.getRequestURI()
        );

        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    // Bean Validation em @RequestBody (@Valid): agrega os erros de campo.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldValidationError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiErrorResponse.FieldValidationError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        log.debug(
                "[GlobalExceptionHandler] - [handleValidation] -> {} erro(s) de validação path={}",
                fieldErrors.size(),
                request.getRequestURI()
        );

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    // Bean Validation em parâmetros (@Validated) fora do corpo.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldValidationError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(v -> new ApiErrorResponse.FieldValidationError(
                        v.getPropertyPath() == null ? null : v.getPropertyPath().toString(),
                        v.getMessage()))
                .toList();

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    // Autorização negada por método/URL (ex.: /api/admin/** sem ROLE_ADMIN).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Access is denied",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // Rota inexistente.
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoHandlerFoundException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                "Resource not found",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // Fallback: nunca expor stack trace/detalhe interno ao cliente.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error(
                "[GlobalExceptionHandler] - [handleUnexpected] -> Erro não tratado path={} type={} msg={}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
