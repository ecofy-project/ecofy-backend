package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.response.ApiErrorResponse;
import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest request(String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }

    @Test
    void handleAuthException_shouldMapErrorCodeToStatusAndBody() {
        AuthException ex = new AuthException(AuthErrorCode.INVALID_CREDENTIALS, "Invalid credentials");

        ResponseEntity<ApiErrorResponse> resp = handler.handleAuthException(ex, request("/auth/api/auth/token"));

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        ApiErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(401, body.status());
        assertEquals("INVALID_CREDENTIALS", body.error());
        assertEquals("Invalid credentials", body.message());
        assertEquals("/auth/api/auth/token", body.path());
        assertNotNull(body.timestamp());
        assertNull(body.fieldErrors());
    }

    @Test
    void handleAuthException_shouldUse403_forAccessLikeCodes() {
        AuthException ex = new AuthException(AuthErrorCode.USER_LOCKED, "User account is locked");

        ResponseEntity<ApiErrorResponse> resp = handler.handleAuthException(ex, request("/auth/api/auth/token"));

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("USER_LOCKED", resp.getBody().error());
    }

    @Test
    void handleValidation_shouldReturn400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "email", "must not be blank"),
                new FieldError("obj", "password", "size must be >= 8")
        ));

        ResponseEntity<ApiErrorResponse> resp = handler.handleValidation(ex, request("/auth/api/register"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        ApiErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("VALIDATION_ERROR", body.error());
        assertNotNull(body.fieldErrors());
        assertEquals(2, body.fieldErrors().size());
        assertTrue(body.fieldErrors().stream().anyMatch(fe -> "email".equals(fe.field())));
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleAccessDenied(new AccessDeniedException("denied"), request("/auth/api/admin/users"));

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("ACCESS_DENIED", resp.getBody().error());
    }

    @Test
    void handleUnexpected_shouldReturn500WithoutLeakingInternalMessage() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleUnexpected(new RuntimeException("NPE at internal.Secret.line42"), request("/auth/api/auth/token"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        ApiErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("INTERNAL_ERROR", body.error());
        assertEquals("An unexpected error occurred", body.message());
        // Não vaza a mensagem interna/stack trace ao cliente.
        assertFalse(body.message().contains("Secret"));
    }
}
