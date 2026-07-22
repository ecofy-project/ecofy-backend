package br.com.ecofy.auth.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.ecofy.auth.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.auth.adapters.in.web.dto.response.ApiErrorResponse;
import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

@DisplayName("Testes unitários do manipulador global de exceções")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve converter a exceção de autenticação conforme o código de erro")
    void handleAuthException_credenciaisInvalidas_deveRetornarStatusECorpoPadronizados() {
        // Arrange
        AuthException exception = new AuthException(
                AuthErrorCode.INVALID_CREDENTIALS,
                "Invalid credentials"
        );

        HttpServletRequest request = request(
                "/api/v1/auth/token",
                "corr-auth-123"
        );

        // Act
        ResponseEntity<ApiErrorResponse> response =
                handler.handleAuthException(exception, request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.status());
        assertEquals("INVALID_CREDENTIALS", body.errorCode());
        assertEquals("Invalid credentials", body.message());
        assertEquals("/api/v1/auth/token", body.path());
        assertEquals("corr-auth-123", body.traceId());
        assertNotNull(body.timestamp());
        assertNotNull(body.details());
        assertTrue(body.details().isEmpty());
    }

    @Test
    @DisplayName("Deve consolidar erros de campos com códigos presentes e ausentes")
    void handleValidation_errosComESemCodigo_deveRetornarDetalhesDeValidacao() {
        // Arrange
        MDC.put(CorrelationId.MDC_KEY, "corr-validation-123");

        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError errorWithoutCode = new FieldError(
                "request",
                "email",
                "must not be blank"
        );

        FieldError errorWithCode = new FieldError(
                "request",
                "password",
                null,
                false,
                new String[]{"Size"},
                null,
                "size must be between 8 and 100"
        );

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(errorWithoutCode, errorWithCode)
        );

        HttpServletRequest request = request(
                "/api/v1/auth/register",
                "   "
        );

        // Act
        ResponseEntity<ApiErrorResponse> response =
                handler.handleValidation(exception, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.status());
        assertEquals("VALIDATION_ERROR", body.errorCode());
        assertEquals("Request validation failed", body.message());
        assertEquals("/api/v1/auth/register", body.path());
        assertEquals("corr-validation-123", body.traceId());
        assertNotNull(body.details());
        assertEquals(2, body.details().size());

        ApiErrorResponse.ErrorDetail emailDetail = body.details().get(0);
        assertEquals("email", emailDetail.field());
        assertEquals("VALIDATION", emailDetail.code());
        assertEquals("must not be blank", emailDetail.message());

        ApiErrorResponse.ErrorDetail passwordDetail = body.details().get(1);
        assertEquals("password", passwordDetail.field());
        assertEquals("Size", passwordDetail.code());
        assertEquals(
                "size must be between 8 and 100",
                passwordDetail.message()
        );
    }

    @Test
    @DisplayName("Deve converter violações com caminhos presentes e ausentes")
    void handleConstraintViolation_caminhosPresenteEAusente_deveRetornarDetalhesDeValidacao() {
        // Arrange
        ConstraintViolation<?> violationWithPath =
                mock(ConstraintViolation.class);
        ConstraintViolation<?> violationWithoutPath =
                mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);

        when(propertyPath.toString()).thenReturn("request.email");
        when(violationWithPath.getPropertyPath()).thenReturn(propertyPath);
        when(violationWithPath.getMessage())
                .thenReturn("must be a valid email");
        when(violationWithoutPath.getPropertyPath()).thenReturn(null);
        when(violationWithoutPath.getMessage())
                .thenReturn("invalid request");

        ConstraintViolationException exception =
                new ConstraintViolationException(
                        Set.of(violationWithPath, violationWithoutPath)
                );

        MDC.put(CorrelationId.MDC_KEY, "corr-constraint-123");

        HttpServletRequest request = request(
                "/api/v1/auth/validate",
                new Object()
        );

        // Act
        ResponseEntity<ApiErrorResponse> response =
                handler.handleConstraintViolation(exception, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.status());
        assertEquals("VALIDATION_ERROR", body.errorCode());
        assertEquals("Request validation failed", body.message());
        assertEquals("/api/v1/auth/validate", body.path());
        assertEquals("corr-constraint-123", body.traceId());
        assertEquals(2, body.details().size());

        assertTrue(
                body.details().stream().anyMatch(detail ->
                        "request.email".equals(detail.field())
                                && "VALIDATION".equals(detail.code())
                                && "must be a valid email".equals(
                                detail.message()
                        )
                )
        );

        assertTrue(
                body.details().stream().anyMatch(detail ->
                        detail.field() == null
                                && "VALIDATION".equals(detail.code())
                                && "invalid request".equals(detail.message())
                )
        );
    }

    @Test
    @DisplayName("Deve retornar acesso negado com status 403")
    void handleAccessDenied_acessoNegado_deveRetornarStatusForbidden() {
        // Arrange
        AccessDeniedException exception =
                new AccessDeniedException("denied");

        MDC.put(CorrelationId.MDC_KEY, "corr-access-123");

        HttpServletRequest request = request(
                "/api/admin/users",
                null
        );

        // Act
        ResponseEntity<ApiErrorResponse> response =
                handler.handleAccessDenied(exception, request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.status());
        assertEquals("ACCESS_DENIED", body.errorCode());
        assertEquals("Access is denied", body.message());
        assertEquals("/api/admin/users", body.path());
        assertEquals("corr-access-123", body.traceId());
        assertNotNull(body.details());
        assertTrue(body.details().isEmpty());
    }

    @Test
    @DisplayName("Deve retornar recurso não encontrado com status 404")
    void handleNotFound_rotaInexistente_deveRetornarStatusNotFound() {
        // Arrange
        NoHandlerFoundException exception =
                new NoHandlerFoundException(
                        "GET",
                        "/api/unknown",
                        HttpHeaders.EMPTY
                );

        HttpServletRequest request = request(
                "/api/unknown",
                "corr-not-found-123"
        );

        // Act
        ResponseEntity<ApiErrorResponse> response =
                handler.handleNotFound(exception, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.status());
        assertEquals("NOT_FOUND", body.errorCode());
        assertEquals("Resource not found", body.message());
        assertEquals("/api/unknown", body.path());
        assertEquals("corr-not-found-123", body.traceId());
        assertNotNull(body.details());
        assertTrue(body.details().isEmpty());
    }

    @Test
    @DisplayName("Deve retornar erro interno sem expor detalhes da exceção")
    void handleUnexpected_excecaoNaoTratada_deveRetornarStatusInternalServerError() {
        // Arrange
        RuntimeException exception =
                new RuntimeException("internal.Secret.line42");

        HttpServletRequest request = request(
                "/api/v1/auth/token",
                null
        );

        // Act
        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnexpected(exception, request);

        // Assert
        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                body.status()
        );
        assertEquals("INTERNAL_ERROR", body.errorCode());
        assertEquals(
                "An unexpected error occurred",
                body.message()
        );
        assertEquals("/api/v1/auth/token", body.path());
        assertNull(body.traceId());
        assertNotNull(body.details());
        assertTrue(body.details().isEmpty());
        assertFalse(body.message().contains("Secret"));
    }

    private HttpServletRequest request(
            String uri,
            Object correlationId
    ) {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getRequestURI()).thenReturn(uri);
        when(request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE))
                .thenReturn(correlationId);

        return request;
    }
}
