package br.com.ecofy.ms_categorization.adapters.in.web.advice;

import br.com.ecofy.ms_categorization.core.application.exception.BusinessValidationException;
import br.com.ecofy.ms_categorization.core.application.exception.CategoryNotFoundException;
import br.com.ecofy.ms_categorization.core.application.exception.RuleNotFoundException;
import br.com.ecofy.ms_categorization.core.application.exception.TransactionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Handler global de erros HTTP do ms-categorization. Traduz exceções de domínio, validação,
 * conflito e falhas inesperadas para {@link ApiErrorResponse} consistente, sem expor stack trace.
 */
@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    // Categoria não encontrada -> 404.
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCategory(CategoryNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", ex.getMessage(), req);
    }

    // Transação não encontrada -> 404.
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTx(TransactionNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", ex.getMessage(), req);
    }

    // Regra não encontrada -> 404.
    @ExceptionHandler(RuleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRule(RuleNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "RULE_NOT_FOUND", ex.getMessage(), req);
    }

    // Regra de negócio inválida -> 422.
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessValidationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_VALIDATION", ex.getMessage(), req);
    }

    // Bean Validation em @RequestBody (@Valid) -> 400 com fieldErrors.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "VALIDATION_ERROR", "Request validation failed", req.getRequestURI(), fieldErrors));
    }

    // Bean Validation em parâmetros (@Validated) -> 400.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", req);
    }

    // Argumento inválido / parâmetro obrigatório ausente / tipo inválido (ex.: UUID malformado) -> 400.
    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", safeMessage(ex.getMessage()), req);
    }

    // Conflito de integridade (ex.: chave de idempotência/unique) -> 409.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("[RestExceptionHandler] - [handleConflict] -> conflito de integridade path={}", req.getRequestURI());
        return build(HttpStatus.CONFLICT, "CONFLICT", "Resource conflict / duplicate key", req);
    }

    // Fallback: nunca expor stack trace/detalhe interno.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("[RestExceptionHandler] - [handleGeneric] -> erro não tratado path={} type={} msg={}",
                req.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req);
    }

    private static ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), code, message, req.getRequestURI()));
    }

    private static String safeMessage(String message) {
        return (message == null || message.isBlank()) ? "Invalid request" : message;
    }
}
