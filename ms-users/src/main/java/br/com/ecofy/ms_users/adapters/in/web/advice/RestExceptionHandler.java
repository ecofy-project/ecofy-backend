package br.com.ecofy.ms_users.adapters.in.web.advice;

import br.com.ecofy.ms_users.adapters.in.web.security.JwtAuthenticatedUserProvider;
import br.com.ecofy.ms_users.core.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

// Centraliza a conversão de exceções no contrato padronizado de erro.
@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    // Converte acessos a recursos de outros usuários em respostas HTTP 403.
    @ExceptionHandler(UserAccessForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            UserAccessForbiddenException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(403)
                .body(ApiErrorResponse.of(
                        403,
                        "USER_ACCESS_FORBIDDEN",
                        "Access to this resource is not allowed",
                        req.getRequestURI()
                ));
    }

    // Converte a ausência de autenticação válida em respostas HTTP 401.
    @ExceptionHandler(
            JwtAuthenticatedUserProvider.NotAuthenticatedException.class
    )
    public ResponseEntity<ApiErrorResponse> handle(
            JwtAuthenticatedUserProvider.NotAuthenticatedException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(401)
                .body(ApiErrorResponse.of(
                        401,
                        "UNAUTHENTICATED",
                        "Authentication is required",
                        req.getRequestURI()
                ));
    }

    // Converte acessos negados em respostas HTTP 403 específicas para o contexto.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            AccessDeniedException ex,
            HttpServletRequest req
    ) {
        String path = req.getRequestURI();
        boolean internal = path != null && path.contains("/internal");

        return ResponseEntity.status(403)
                .body(ApiErrorResponse.of(
                        403,
                        internal
                                ? "INTERNAL_ENDPOINT_FORBIDDEN"
                                : "USER_ACCESS_FORBIDDEN",
                        "Access to this resource is not allowed",
                        path
                ));
    }

    // Converte conflitos de atualização concorrente em respostas HTTP 409.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            OptimisticLockingFailureException ex,
            HttpServletRequest req
    ) {
        log.warn(
                "[RestExceptionHandler] -> Conflito de concorrência path={}",
                req.getRequestURI()
        );

        return ResponseEntity.status(409)
                .body(ApiErrorResponse.of(
                        409,
                        "USER_PROFILE_CONCURRENT_UPDATE",
                        "O perfil foi alterado por outra operação. Atualize os dados e tente novamente.",
                        req.getRequestURI()
                ));
    }

    // Converte violações de integridade em respostas HTTP 409 sem expor detalhes internos.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            DataIntegrityViolationException ex,
            HttpServletRequest req
    ) {
        log.warn(
                "[RestExceptionHandler] -> Violação de integridade path={} cause={}",
                req.getRequestURI(),
                ex.getMostSpecificCause().getClass().getSimpleName()
        );

        return ResponseEntity.status(409)
                .body(ApiErrorResponse.of(
                        409,
                        "USER_PROFILE_ALREADY_EXISTS",
                        "A user profile already exists for this user",
                        req.getRequestURI()
                ));
    }

    // Converte perfis inexistentes em respostas HTTP 404.
    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            UserProfileNotFoundException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(404)
                .body(ApiErrorResponse.of(
                        404,
                        "USER_PROFILE_NOT_FOUND",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte conexões inexistentes em respostas HTTP 404.
    @ExceptionHandler(ConnectionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            ConnectionNotFoundException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(404)
                .body(ApiErrorResponse.of(
                        404,
                        "CONNECTION_NOT_FOUND",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte violações de idempotência em respostas HTTP 409.
    @ExceptionHandler(IdempotencyViolationException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            IdempotencyViolationException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.status(409)
                .body(ApiErrorResponse.of(
                        409,
                        "IDEMPOTENCY_VIOLATION",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte parâmetros de paginação inválidos em respostas HTTP 400.
    @ExceptionHandler(
            br.com.ecofy.ms_users.core.application.pagination
                    .InvalidPaginationException.class
    )
    public ResponseEntity<ApiErrorResponse> handle(
            br.com.ecofy.ms_users.core.application.pagination
                    .InvalidPaginationException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        400,
                        "PAGINATION_PARAMETER_INVALID",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte violações de regras de negócio em respostas HTTP 400.
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            BusinessValidationException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        400,
                        "VALIDATION_ERROR",
                        ex.getMessage(),
                        req.getRequestURI()
                ));
    }

    // Converte erros de campos em respostas HTTP 400 com detalhes controlados.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {
        List<ApiErrorResponse.ErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ApiErrorResponse.ErrorDetail(
                        fieldError.getField(),
                        fieldError.getCode() == null
                                ? "VALIDATION"
                                : fieldError.getCode(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        400,
                        "VALIDATION_ERROR",
                        "A requisição contém dados inválidos.",
                        req.getRequestURI(),
                        details
                ));
    }

    // Converte exceções não tratadas sem expor informações internas ao cliente.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handle(
            Exception ex,
            HttpServletRequest req
    ) {
        log.error(
                "[RestExceptionHandler] -> Erro não tratado path={} type={}",
                req.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex
        );

        return ResponseEntity.internalServerError()
                .body(ApiErrorResponse.of(
                        500,
                        "INTERNAL_USER_ERROR",
                        "An unexpected error occurred",
                        req.getRequestURI()
                ));
    }
}
