package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.correlation.CorrelationId;
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

// Centraliza a conversão de exceções em respostas HTTP padronizadas.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Converte exceções de autenticação conforme o código de erro definido.
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(
            AuthException ex,
            HttpServletRequest request
    ) {
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
                request.getRequestURI(),
                traceId(request)
        );

        return ResponseEntity.status(status).body(body);
    }

    // Consolida os erros de validação encontrados no corpo da requisição.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.ErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiErrorResponse.ErrorDetail(
                        fe.getField(),
                        fe.getCode() == null ? "VALIDATION" : fe.getCode(),
                        fe.getDefaultMessage()))
                .toList();

        log.debug(
                "[GlobalExceptionHandler] - [handleValidation] -> {} erro(s) de validação path={}",
                details.size(),
                request.getRequestURI()
        );

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                traceId(request),
                details
        );

        return ResponseEntity.badRequest().body(body);
    }

    // Converte violações de parâmetros em uma resposta de validação.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.ErrorDetail> details = ex.getConstraintViolations()
                .stream()
                .map(v -> new ApiErrorResponse.ErrorDetail(
                        v.getPropertyPath() == null
                                ? null
                                : v.getPropertyPath().toString(),
                        "VALIDATION",
                        v.getMessage()))
                .toList();

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                traceId(request),
                details
        );

        return ResponseEntity.badRequest().body(body);
    }

    // Converte recusas de autorização em uma resposta de acesso negado.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Access is denied",
                request.getRequestURI(),
                traceId(request)
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(body);
    }

    // Converte tentativas de acesso a rotas inexistentes em uma resposta de recurso não encontrado.
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoHandlerFoundException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                "Resource not found",
                request.getRequestURI(),
                traceId(request)
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(body);
    }

    // Registra falhas inesperadas e retorna uma resposta sem detalhes internos.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
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
                request.getRequestURI(),
                traceId(request)
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    // Resolve o identificador de rastreamento associado à requisição.
    private static String traceId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE);

        if (attr instanceof String s && !s.isBlank()) {
            return s;
        }

        return CorrelationId.current();
    }
}
