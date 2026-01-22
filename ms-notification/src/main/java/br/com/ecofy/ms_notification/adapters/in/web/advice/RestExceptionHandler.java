package br.com.ecofy.ms_notification.adapters.in.web.advice;

import br.com.ecofy.ms_notification.core.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class RestExceptionHandler {

    // Converte TemplateNotFoundException em 404 padronizado, retornando um ApiErrorResponse com código de razão e path da requisição.
    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTemplateNotFound(TemplateNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(NOT_FOUND).body(ApiErrorResponse.of("TEMPLATE_NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    // Converte NotificationNotFoundException em 404 padronizado, informando que a notificação solicitada não foi encontrada.
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotificationNotFound(NotificationNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(NOT_FOUND).body(ApiErrorResponse.of("NOTIFICATION_NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    // Converte violações de idempotência em 409, sinalizando conflito por requisição/evento duplicado.
    @ExceptionHandler(IdempotencyViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotency(IdempotencyViolationException ex, HttpServletRequest req) {
        return ResponseEntity.status(CONFLICT).body(ApiErrorResponse.of("IDEMPOTENCY_VIOLATION", ex.getMessage(), req.getRequestURI()));
    }

    // Converte erros de validação de regra de negócio em 400, retornando motivo e mensagem descritiva para o cliente.
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessValidationException ex, HttpServletRequest req) {
        return ResponseEntity.status(BAD_REQUEST).body(ApiErrorResponse.of("BUSINESS_VALIDATION", ex.getMessage(), req.getRequestURI()));
    }

    // Converte falhas do provedor externo de entrega em 502, indicando erro ao integrar/entregar por dependência externa.
    @ExceptionHandler(DeliveryProviderException.class)
    public ResponseEntity<ApiErrorResponse> handleProvider(DeliveryProviderException ex, HttpServletRequest req) {
        return ResponseEntity.status(BAD_GATEWAY).body(ApiErrorResponse.of("DELIVERY_PROVIDER_ERROR", ex.getMessage(), req.getRequestURI()));
    }

    // Converte qualquer exceção não tratada em 500, evitando vazamento de stacktrace e padronizando a resposta de erro.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(ApiErrorResponse.of("INTERNAL_ERROR", ex.getMessage(), req.getRequestURI()));
    }

}
