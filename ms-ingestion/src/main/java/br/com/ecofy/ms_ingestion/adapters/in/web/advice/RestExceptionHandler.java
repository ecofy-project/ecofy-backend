package br.com.ecofy.ms_ingestion.adapters.in.web.advice;

import br.com.ecofy.ms_ingestion.core.application.exception.IngestionErrorCode;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Handler global de erros HTTP do ms-ingestion. Traduz exceções de domínio/validação/upload
 * para {@link ApiErrorResponse} consistente, sem expor stack trace nem caminho local de arquivo.
 */
@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    // Exceções de domínio/aplicação: status + code vêm do IngestionErrorCode.
    @ExceptionHandler(IngestionException.class)
    public ResponseEntity<ApiErrorResponse> handleIngestion(IngestionException ex, HttpServletRequest req) {
        IngestionErrorCode ec = ex.getErrorCode();
        HttpStatus status = ec != null ? ec.getHttpStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        String code = ec != null ? ec.getCode() : "INGESTION_ERROR";

        log.warn("[RestExceptionHandler] - [handleIngestion] -> code={} status={} path={}",
                code, status.value(), req.getRequestURI());

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), code, safeMessage(ex.getMessage()), req.getRequestURI()));
    }

    // Bean Validation em @RequestBody (@Valid).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "VALIDATION_ERROR", "Request validation failed", req.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "VALIDATION_ERROR", "Request validation failed", req.getRequestURI()));
    }

    // Parte multipart ausente (ex.: campo "file" não enviado) ou request param faltando.
    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> handleMissingPart(Exception ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "MISSING_REQUIRED_PART", "Required request part/parameter is missing", req.getRequestURI()));
    }

    // Arquivo maior que o limite do multipart do servidor.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiErrorResponse.of(HttpStatus.PAYLOAD_TOO_LARGE.value(), "FILE_TOO_LARGE",
                        "Uploaded file exceeds the maximum allowed size", req.getRequestURI()));
    }

    // Demais erros de upload/multipart.
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipart(MultipartException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "INVALID_MULTIPART", "Invalid multipart/upload request", req.getRequestURI()));
    }

    // Tipo de arquivo inválido inferido / argumento inválido (ex.: guessType sem sufixo conhecido).
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "INVALID_REQUEST", safeMessage(ex.getMessage()), req.getRequestURI()));
    }

    // Fallback: nunca expor stack trace/detalhe interno.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("[RestExceptionHandler] - [handleUnexpected] -> path={} type={} msg={}",
                req.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred", req.getRequestURI()));
    }

    // Evita vazar caminho local de arquivo em mensagens de erro retornadas ao cliente.
    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Request could not be processed";
        }
        String m = message;
        // Remove referências a caminhos locais (Windows/Unix) por segurança.
        if (m.matches(".*([A-Za-z]:\\\\|/var/|/home/|/tmp/|\\./data).*")) {
            return "Request could not be processed";
        }
        return m;
    }
}
