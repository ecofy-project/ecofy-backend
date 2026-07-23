package br.com.ecofy.ms_notification.adapters.in.web.advice;

import br.com.ecofy.ms_notification.core.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.*;

// Centraliza a conversão de exceções em respostas HTTP padronizadas.
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    private static final String GENERIC_ERROR_MESSAGE =
            "Erro interno inesperado ao processar a notificação.";

    // Converte templates não encontrados em respostas HTTP 404.
    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTemplateNotFound(
            TemplateNotFoundException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(NOT_FOUND)
                .body(ApiErrorResponse.of(
                        "TEMPLATE_NOT_FOUND",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte notificações não encontradas em respostas HTTP 404.
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotificationNotFound(
            NotificationNotFoundException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(NOT_FOUND)
                .body(ApiErrorResponse.of(
                        "NOTIFICATION_NOT_FOUND",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte violações de idempotência em respostas HTTP 409.
    @ExceptionHandler(IdempotencyViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotency(
            IdempotencyViolationException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(CONFLICT)
                .body(ApiErrorResponse.of(
                        "IDEMPOTENCY_VIOLATION",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte violações de regras de negócio em respostas HTTP 400.
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessValidationException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        "BUSINESS_VALIDATION",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte falhas de provedores externos em respostas HTTP 502.
    @ExceptionHandler(DeliveryProviderException.class)
    public ResponseEntity<ApiErrorResponse> handleProvider(
            DeliveryProviderException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(BAD_GATEWAY)
                .body(ApiErrorResponse.of(
                        "DELIVERY_PROVIDER_ERROR",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte erros de validação do corpo em respostas HTTP 400.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {
        String detail = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Requisição inválida");

        return ResponseEntity.status(BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        "VALIDATION_ERROR",
                        detail,
                        req.getRequestURI()
                ));
    }

    // Converte violações de restrições em respostas HTTP 400.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        "VALIDATION_ERROR",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte corpos ilegíveis ou malformados em respostas HTTP 400.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        "MALFORMED_REQUEST",
                        "Corpo da requisição inválido ou malformado.",
                        req.getRequestURI()
                ));
    }

    // Converte exceções não tratadas sem expor detalhes internos ao cliente.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest req
    ) {
        String traceId = MDC.get("correlationId");

        log.error(
                "[RestExceptionHandler] - [handleGeneric] -> unexpected error path={} traceId={} type={} message={}",
                req.getRequestURI(),
                traceId,
                ex.getClass().getName(),
                ex.getMessage(),
                ex
        );

        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(
                        "INTERNAL_ERROR",
                        GENERIC_ERROR_MESSAGE,
                        req.getRequestURI(),
                        traceId
                ));
    }
}
