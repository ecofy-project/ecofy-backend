package br.com.ecofy.ms_ingestion.adapters.in.web.advice;

import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.ErrorDetail;
import br.com.ecofy.ms_ingestion.core.application.exception.FileColumnLimitExceededException;
import br.com.ecofy.ms_ingestion.core.application.exception.FileLineLimitExceededException;
import br.com.ecofy.ms_ingestion.core.application.exception.FileLineTooLongException;
import br.com.ecofy.ms_ingestion.core.application.exception.IdempotencyKeyPayloadMismatchException;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportAccessForbiddenException;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportAlreadyProcessedException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionErrorCode;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import br.com.ecofy.ms_ingestion.core.application.exception.InvalidFileHeaderException;
import br.com.ecofy.ms_ingestion.core.application.exception.PaginationParameterInvalidException;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

// Centraliza a conversão de exceções em respostas HTTP seguras.
@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    private final IngestionProperties properties;

    public RestExceptionHandler(IngestionProperties properties) {
        this.properties = properties;
    }

    // Traduz replays idempotentes em conflito e referencia o job existente.
    @ExceptionHandler(ImportAlreadyProcessedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyProcessed(ImportAlreadyProcessedException ex,
                                                                   HttpServletRequest req) {
        log.info("[RestExceptionHandler] - [handleAlreadyProcessed] -> replay idempotente existingJobId={}",
                ex.getExistingJobId());

        List<ApiErrorResponse.Detail> details = List.of(ApiErrorResponse.Detail.ofField(
                "importJobId",
                "ALREADY_IMPORTED",
                "Este arquivo já foi importado. Consulte o job existente."));

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header("Location", "/api/import/jobs/" + ex.getExistingJobId())
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        IngestionErrorCode.IMPORT_ALREADY_PROCESSED.getCode(),
                        "Este arquivo já foi importado.",
                        req.getRequestURI(),
                        details));
    }

    @ExceptionHandler(IdempotencyKeyPayloadMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyMismatch(IdempotencyKeyPayloadMismatchException ex,
                                                                      HttpServletRequest req) {
        return build(ex.getErrorCode(),
                "A Idempotency-Key informada já foi usada com um arquivo diferente.",
                req,
                List.of());
    }

    // Traduz acessos negados sem revelar a existência da importação.
    @ExceptionHandler(ImportAccessForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ImportAccessForbiddenException ex,
                                                            HttpServletRequest req) {
        log.warn("[RestExceptionHandler] - [handleForbidden] -> acesso negado path={}", req.getRequestURI());
        return build(ex.getErrorCode(), "Acesso negado a esta importação.", req, List.of());
    }

    @ExceptionHandler(InvalidFileHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidHeader(InvalidFileHeaderException ex,
                                                                HttpServletRequest req) {
        return build(ex.getErrorCode(),
                "O cabeçalho do arquivo não corresponde ao formato esperado.",
                req,
                toApiDetails(ex.getDetails()));
    }

    @ExceptionHandler(PaginationParameterInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handlePagination(PaginationParameterInvalidException ex,
                                                             HttpServletRequest req) {
        return build(ex.getErrorCode(),
                "Parâmetro de paginação inválido.",
                req,
                toApiDetails(ex.getDetails()));
    }

    // Informa o limite configurado quando o arquivo excede a quantidade de linhas.
    @ExceptionHandler(FileLineLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleLineLimit(FileLineLimitExceededException ex,
                                                            HttpServletRequest req) {
        return build(ex.getErrorCode(),
                "O arquivo excede o número máximo de linhas permitido.",
                req,
                List.of(ApiErrorResponse.Detail.ofField("maxLines", "LIMIT", String.valueOf(ex.getMaxLines()))));
    }

    @ExceptionHandler(FileLineTooLongException.class)
    public ResponseEntity<ApiErrorResponse> handleLineTooLong(FileLineTooLongException ex,
                                                              HttpServletRequest req) {
        return build(ex.getErrorCode(),
                "Uma linha do arquivo excede o tamanho máximo permitido.",
                req,
                List.of(ApiErrorResponse.Detail.ofField(
                        "maxLineLength", "LIMIT", String.valueOf(ex.getMaxLineLength()))));
    }

    @ExceptionHandler(FileColumnLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleColumnLimit(FileColumnLimitExceededException ex,
                                                              HttpServletRequest req) {
        return build(ex.getErrorCode(),
                "O arquivo excede o número máximo de colunas permitido.",
                req,
                List.of(ApiErrorResponse.Detail.ofField(
                        "maxColumns", "LIMIT", String.valueOf(ex.getMaxColumns()))));
    }

    // Traduz falhas da aplicação conforme o código de erro definido.
    @ExceptionHandler(IngestionException.class)
    public ResponseEntity<ApiErrorResponse> handleIngestion(IngestionException ex, HttpServletRequest req) {
        IngestionErrorCode ec = ex.getErrorCode() != null
                ? ex.getErrorCode()
                : IngestionErrorCode.INTERNAL_INGESTION_ERROR;

        log.warn("[RestExceptionHandler] - [handleIngestion] -> code={} status={} path={} detail={}",
                ec.getCode(), ec.getHttpStatus().value(), req.getRequestURI(), ex.getDetail());

        return build(ec, safeMessage(ex.getMessage()), req, List.of());
    }

    // Traduz transições inválidas sem expor detalhes internos.
    @ExceptionHandler(ImportJob.IllegalStateTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalTransition(ImportJob.IllegalStateTransitionException ex,
                                                                    HttpServletRequest req) {
        log.error("[RestExceptionHandler] - [handleIllegalTransition] -> path={} msg={}",
                req.getRequestURI(), ex.getMessage());
        return build(IngestionErrorCode.INTERNAL_INGESTION_ERROR,
                "Ocorreu um erro inesperado.", req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest req) {
        List<ApiErrorResponse.Detail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ApiErrorResponse.Detail.ofField(fe.getField(), "INVALID", fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "VALIDATION_ERROR", "Falha de validação da requisição.",
                        req.getRequestURI(), details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex,
                                                             HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "VALIDATION_ERROR", "Falha de validação da requisição.",
                        req.getRequestURI()));
    }

    // Traduz ausências de parâmetros ou partes multipart em arquivo obrigatório.
    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> handleMissingPart(Exception ex, HttpServletRequest req) {
        return build(IngestionErrorCode.FILE_REQUIRED,
                "O arquivo é obrigatório.", req, List.of());
    }

    // Informa o limite configurado quando o upload excede o tamanho permitido.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex,
                                                            HttpServletRequest req) {
        long maxBytes = properties.getUpload().getMaxFileSize().toBytes();

        log.warn("[RestExceptionHandler] - [handleMaxUpload] -> upload rejeitado por tamanho path={} maxBytes={}",
                req.getRequestURI(), maxBytes);

        return build(IngestionErrorCode.FILE_SIZE_LIMIT_EXCEEDED,
                "O arquivo excede o tamanho máximo permitido.",
                req,
                List.of(ApiErrorResponse.Detail.ofField("maxFileSizeBytes", "LIMIT", String.valueOf(maxBytes))));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipart(MultipartException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "INVALID_MULTIPART", "Requisição multipart inválida.",
                        req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(IngestionErrorCode.IMPORT_ACCESS_FORBIDDEN, "Acesso negado.", req, List.of());
    }

    // Traduz falhas de autenticação internas em respostas não autorizadas.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex,
                                                                 HttpServletRequest req) {
        log.warn("[RestExceptionHandler] - [handleAuthentication] -> autenticação inválida path={} type={}",
                req.getRequestURI(), ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                        "Autenticação inválida ou ausente.", req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                  HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "INVALID_REQUEST", safeMessage(ex.getMessage()),
                        req.getRequestURI()));
    }

    // Converte falhas inesperadas em respostas genéricas.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("[RestExceptionHandler] - [handleUnexpected] -> path={} type={} msg={}",
                req.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        return build(IngestionErrorCode.INTERNAL_INGESTION_ERROR, "Ocorreu um erro inesperado.", req, List.of());
    }

    private ResponseEntity<ApiErrorResponse> build(IngestionErrorCode ec,
                                                   String message,
                                                   HttpServletRequest req,
                                                   List<ApiErrorResponse.Detail> details) {
        HttpStatus status = ec.getHttpStatus();
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), ec.getCode(), message, req.getRequestURI(), details));
    }

    private static List<ApiErrorResponse.Detail> toApiDetails(List<ErrorDetail> details) {
        return details.stream()
                .map(d -> new ApiErrorResponse.Detail(d.row(), d.field(), d.code(), d.message()))
                .toList();
    }

    // Sanitiza mensagens para impedir a exposição de caminhos locais.
    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Não foi possível processar a requisição.";
        }
        if (message.matches(".*([A-Za-z]:\\\\|/var/|/home/|/tmp/|\\./data).*")) {
            return "Não foi possível processar a requisição.";
        }
        return message;
    }
}
