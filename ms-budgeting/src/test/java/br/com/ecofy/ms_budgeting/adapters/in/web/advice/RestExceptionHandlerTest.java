package br.com.ecofy.ms_budgeting.adapters.in.web.advice;

import br.com.ecofy.ms_budgeting.core.domain.exception.BudgetAlreadyExistsException;
import br.com.ecofy.ms_budgeting.core.domain.exception.BudgetNotFoundException;
import br.com.ecofy.ms_budgeting.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_budgeting.core.domain.exception.IdempotencyViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void shouldHandleBudgetNotFoundExceptionWithTraceId() {
        MockHttpServletRequest request = request("/api/v1/budgets/123");
        request.addHeader("X-Trace-Id", "trace-001");

        UUID budgetId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        BudgetNotFoundException exception =
                new BudgetNotFoundException(budgetId);

        ResponseEntity<ApiErrorResponse> response =
                handler.handleNotFound(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                "/api/v1/budgets/123",
                "trace-001",
                Map.of()
        );
    }

    @Test
    void shouldHandleBudgetAlreadyExistsExceptionWithCorrelationIdFallback() {
        MockHttpServletRequest request = request("/api/v1/budgets");
        request.addHeader("X-Trace-Id", "   ");
        request.addHeader("X-Correlation-Id", "correlation-001");

        BudgetAlreadyExistsException exception =
                new BudgetAlreadyExistsException("Budget already exists");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleConflict(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "/api/v1/budgets",
                "correlation-001",
                Map.of("reason", "BUDGET_ALREADY_EXISTS")
        );
    }

    @Test
    void shouldHandleIdempotencyViolationException() {
        MockHttpServletRequest request = request("/api/v1/budgets/process");
        request.addHeader("X-Trace-Id", "trace-002");

        IdempotencyViolationException exception =
                new IdempotencyViolationException("Duplicated idempotency key");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleIdempotency(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "/api/v1/budgets/process",
                "trace-002",
                Map.of("reason", "IDEMPOTENCY_VIOLATION")
        );
    }

    @Test
    void shouldHandleBusinessValidationExceptionWithoutTraceId() {
        MockHttpServletRequest request = request("/api/v1/budgets");

        BusinessValidationException exception =
                new BusinessValidationException("Invalid business rule");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleBusiness(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "Invalid business rule",
                "/api/v1/budgets",
                null,
                Map.of("reason", "BUSINESS_VALIDATION")
        );
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() throws Exception {
        MockHttpServletRequest request = request("/api/v1/budgets");
        request.addHeader("X-Trace-Id", "trace-validation-001");

        MethodArgumentNotValidException exception =
                methodArgumentNotValidException();

        ResponseEntity<ApiErrorResponse> response =
                handler.handleValidation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertNotNull(body.timestamp());
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.status());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), body.error());
        assertEquals("Invalid payload", body.message());
        assertEquals("/api/v1/budgets", body.path());
        assertEquals("trace-validation-001", body.traceId());
        assertNotNull(body.details());

        assertTrue(body.details().containsKey("fields"));

        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) body.details().get("fields");

        assertEquals(2, fields.size());
        assertEquals("must not be null", fields.get("amount"));
        assertEquals("must not be blank", fields.get("currency"));
    }

    @Test
    void shouldHandleGenericException() {
        MockHttpServletRequest request = request("/api/v1/budgets/recalculate");
        request.addHeader("X-Correlation-Id", "correlation-generic-001");

        Exception exception = new RuntimeException("Database unavailable");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleGeneric(exception, request);

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                "/api/v1/budgets/recalculate",
                "correlation-generic-001",
                Map.of()
        );
    }

    @Test
    void shouldPreferTraceIdOverCorrelationIdWhenBothHeadersExist() {
        MockHttpServletRequest request = request("/api/v1/budgets/123");
        request.addHeader("X-Trace-Id", "trace-priority-001");
        request.addHeader("X-Correlation-Id", "correlation-ignored-001");

        UUID budgetId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        BudgetNotFoundException exception =
                new BudgetNotFoundException(budgetId);

        ResponseEntity<ApiErrorResponse> response =
                handler.handleNotFound(exception, request);

        assertNotNull(response.getBody());
        assertEquals("trace-priority-001", response.getBody().traceId());
        assertEquals(exception.getMessage(), response.getBody().message());
    }

    private static void assertErrorResponse(
            ResponseEntity<ApiErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedMessage,
            String expectedPath,
            String expectedTraceId,
            Map<String, Object> expectedDetails
    ) {
        assertEquals(expectedStatus, response.getStatusCode());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertNotNull(body.timestamp());
        assertEquals(expectedStatus.value(), body.status());
        assertEquals(expectedStatus.getReasonPhrase(), body.error());
        assertEquals(expectedMessage, body.message());
        assertEquals(expectedPath, body.path());
        assertEquals(expectedTraceId, body.traceId());
        assertEquals(expectedDetails, body.details());
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }

    private static MethodArgumentNotValidException methodArgumentNotValidException() throws Exception {
        DummyPayload payload = new DummyPayload(null, null);

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(payload, "payload");

        bindingResult.addError(
                new FieldError(
                        "payload",
                        "amount",
                        "must not be null"
                )
        );

        bindingResult.addError(
                new FieldError(
                        "payload",
                        "currency",
                        "must not be blank"
                )
        );

        Method method = RestExceptionHandlerTest.class.getDeclaredMethod(
                "dummyEndpoint",
                DummyPayload.class
        );

        MethodParameter methodParameter = new MethodParameter(method, 0);

        return new MethodArgumentNotValidException(methodParameter, bindingResult);
    }

    @SuppressWarnings("unused")
    private void dummyEndpoint(DummyPayload payload) {
        // usado apenas para criar MethodParameter no teste
    }

    private record DummyPayload(
            String amount,
            String currency
    ) {
    }
}