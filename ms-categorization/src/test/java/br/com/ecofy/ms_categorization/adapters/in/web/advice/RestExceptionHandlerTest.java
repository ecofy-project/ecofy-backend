package br.com.ecofy.ms_categorization.adapters.in.web.advice;

import br.com.ecofy.ms_categorization.core.application.exception.BusinessValidationException;
import br.com.ecofy.ms_categorization.core.application.exception.CategoryNotFoundException;
import br.com.ecofy.ms_categorization.core.application.exception.RuleNotFoundException;
import br.com.ecofy.ms_categorization.core.application.exception.TransactionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    private HttpServletRequest req(String uri) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn(uri);
        return r;
    }

    @Test
    void categoryNotFound_maps404() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleCategory(new CategoryNotFoundException(UUID.randomUUID()), req("/api/categorization/v1/x"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("CATEGORY_NOT_FOUND", resp.getBody().code());
        assertEquals(404, resp.getBody().status());
    }

    @Test
    void transactionNotFound_maps404() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleTx(new TransactionNotFoundException(UUID.randomUUID()), req("/x"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("TRANSACTION_NOT_FOUND", resp.getBody().code());
    }

    @Test
    void ruleNotFound_maps404() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleRule(new RuleNotFoundException(UUID.randomUUID()), req("/x"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("RULE_NOT_FOUND", resp.getBody().code());
    }

    @Test
    void businessValidation_maps422() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleBusiness(new BusinessValidationException("bad rule"), req("/x"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("BUSINESS_VALIDATION", resp.getBody().code());
    }

    @Test
    void illegalArgument_maps400() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleBadRequest(new IllegalArgumentException("nope"), req("/x"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("INVALID_REQUEST", resp.getBody().code());
    }

    @Test
    void dataIntegrity_maps409() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleConflict(new DataIntegrityViolationException("dup"), req("/x"));
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("CONFLICT", resp.getBody().code());
    }

    @Test
    void generic_maps500_withoutLeakingDetails() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleGeneric(new RuntimeException("NPE at Secret.line42"), req("/x"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("INTERNAL_ERROR", resp.getBody().code());
        assertEquals("An unexpected error occurred", resp.getBody().message());
        assertFalse(resp.getBody().message().contains("Secret"));
    }
}
