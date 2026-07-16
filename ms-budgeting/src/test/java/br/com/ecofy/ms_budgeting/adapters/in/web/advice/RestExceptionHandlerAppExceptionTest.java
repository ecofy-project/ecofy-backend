package br.com.ecofy.ms_budgeting.adapters.in.web.advice;

import br.com.ecofy.ms_budgeting.core.application.exception.BudgetingProcessingException;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidCurrencyCodeException;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidFieldException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Correção Dia 6: exceções de APLICAÇÃO (core.application.exception.*) antes caíam no fallback 500 genérico.
 * Agora validação -> 400 e demais processamento -> 500 com code padronizado.
 */
class RestExceptionHandlerAppExceptionTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    private HttpServletRequest req() {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn("/api/budgeting/v1/budgets");
        return r;
    }

    @Test
    void invalidFieldException_shouldMapTo400() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleAppValidation(InvalidFieldException.required("amount"), req());

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(400, resp.getBody().status());
        assertEquals("BUDGETING_VALIDATION_ERROR", resp.getBody().details().get("code"));
    }

    @Test
    void invalidCurrencyCodeException_shouldMapTo400() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleAppValidation(new InvalidCurrencyCodeException("XXX", new RuntimeException()), req());

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void processingException_shouldMapTo500_withCode_noStackTrace() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleApplication(new BudgetingProcessingException("boom"), req());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("Processing error", resp.getBody().message());
        assertEquals("BUDGETING_PROCESSING_ERROR", resp.getBody().details().get("code"));
    }
}
