package br.com.ecofy.ms_budgeting.adapters.in.web.advice;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorResponseTest {

    private static final Instant TIMESTAMP =
            Instant.parse("2026-06-25T10:30:00Z");

    private static final Map<String, Object> DETAILS = Map.of(
            "field", "amount",
            "reason", "must be positive"
    );

    @Test
    void shouldCreateApiErrorResponseWithAllFields() {
        ApiErrorResponse response = new ApiErrorResponse(
                TIMESTAMP,
                400,
                "Bad Request",
                "Invalid request body",
                "/api/v1/budgets",
                "trace-001",
                DETAILS
        );

        assertEquals(TIMESTAMP, response.timestamp());
        assertEquals(400, response.status());
        assertEquals("Bad Request", response.error());
        assertEquals("Invalid request body", response.message());
        assertEquals("/api/v1/budgets", response.path());
        assertEquals("trace-001", response.traceId());
        assertEquals(DETAILS, response.details());
    }

    @Test
    void shouldCreateApiErrorResponseWithNullFields() {
        ApiErrorResponse response = new ApiErrorResponse(
                null,
                0,
                null,
                null,
                null,
                null,
                null
        );

        assertNull(response.timestamp());
        assertEquals(0, response.status());
        assertNull(response.error());
        assertNull(response.message());
        assertNull(response.path());
        assertNull(response.traceId());
        assertNull(response.details());
    }

    @Test
    void shouldCompareApiErrorResponseByAllRecordComponents() {
        ApiErrorResponse response = new ApiErrorResponse(
                TIMESTAMP,
                400,
                "Bad Request",
                "Invalid request body",
                "/api/v1/budgets",
                "trace-001",
                DETAILS
        );

        ApiErrorResponse sameResponse = new ApiErrorResponse(
                TIMESTAMP,
                400,
                "Bad Request",
                "Invalid request body",
                "/api/v1/budgets",
                "trace-001",
                DETAILS
        );

        ApiErrorResponse differentResponse = new ApiErrorResponse(
                TIMESTAMP,
                404,
                "Not Found",
                "Budget not found",
                "/api/v1/budgets/123",
                "trace-001",
                DETAILS
        );

        assertEquals(response, response);
        assertEquals(response, sameResponse);
        assertNotEquals(response, differentResponse);
        assertNotEquals(response, null);
        assertNotEquals(response, "not-an-api-error-response");
    }

    @Test
    void shouldGenerateHashCodeUsingAllRecordComponents() {
        ApiErrorResponse response = new ApiErrorResponse(
                TIMESTAMP,
                400,
                "Bad Request",
                "Invalid request body",
                "/api/v1/budgets",
                "trace-001",
                DETAILS
        );

        ApiErrorResponse sameResponse = new ApiErrorResponse(
                TIMESTAMP,
                400,
                "Bad Request",
                "Invalid request body",
                "/api/v1/budgets",
                "trace-001",
                DETAILS
        );

        assertEquals(response, sameResponse);
        assertEquals(response.hashCode(), sameResponse.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenStatusChanges() {
        ApiErrorResponse response = baseResponse();

        ApiErrorResponse differentResponse = new ApiErrorResponse(
                TIMESTAMP,
                500,
                "Bad Request",
                "Invalid request body",
                "/api/v1/budgets",
                "trace-001",
                DETAILS
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenDetailsChange() {
        ApiErrorResponse response = baseResponse();

        ApiErrorResponse differentResponse = new ApiErrorResponse(
                TIMESTAMP,
                400,
                "Bad Request",
                "Invalid request body",
                "/api/v1/budgets",
                "trace-001",
                Map.of("field", "currency")
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldReturnToStringWithRecordComponents() {
        ApiErrorResponse response = baseResponse();

        String result = response.toString();

        assertTrue(result.contains("ApiErrorResponse"));
        assertTrue(result.contains("timestamp=" + TIMESTAMP));
        assertTrue(result.contains("status=400"));
        assertTrue(result.contains("error=Bad Request"));
        assertTrue(result.contains("message=Invalid request body"));
        assertTrue(result.contains("path=/api/v1/budgets"));
        assertTrue(result.contains("traceId=trace-001"));
        assertTrue(result.contains("details="));
    }

    private static ApiErrorResponse baseResponse() {
        return new ApiErrorResponse(
                TIMESTAMP,
                400,
                "Bad Request",
                "Invalid request body",
                "/api/v1/budgets",
                "trace-001",
                DETAILS
        );
    }
}