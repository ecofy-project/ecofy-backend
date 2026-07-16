package br.com.ecofy.ms_ingestion.adapters.in.web.advice;

import br.com.ecofy.ms_ingestion.core.application.exception.IngestionErrorCode;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
    void handleIngestion_shouldMapErrorCodeToStatusAndBody() {
        var ex = new IngestionException(IngestionErrorCode.IMPORT_JOB_NOT_FOUND, "ImportJob not found");

        ResponseEntity<ApiErrorResponse> resp = handler.handleIngestion(ex, req("/api/import/jobs/x"));

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("IMPORT_JOB_NOT_FOUND", resp.getBody().code());
        assertEquals(404, resp.getBody().status());
        assertEquals("/api/import/jobs/x", resp.getBody().path());
    }

    @Test
    void handleIngestion_fileTooLarge_shouldReturn413() {
        var ex = new IngestionException(IngestionErrorCode.FILE_TOO_LARGE, "too large");

        ResponseEntity<ApiErrorResponse> resp = handler.handleIngestion(ex, req("/api/import/file"));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, resp.getStatusCode());
        assertEquals("FILE_TOO_LARGE", resp.getBody().code());
    }

    @Test
    void handleIngestion_shouldNotLeakLocalFilePath() {
        var ex = new IngestionException(IngestionErrorCode.STORAGE_ERROR, "Failed writing C:\\var\\lib\\ecofy\\secret.csv");

        ResponseEntity<ApiErrorResponse> resp = handler.handleIngestion(ex, req("/api/import/file"));

        assertFalse(resp.getBody().message().contains("secret.csv"));
        assertFalse(resp.getBody().message().contains("C:\\"));
    }

    @Test
    void handleIllegalArgument_shouldReturn400() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleIllegalArgument(new IllegalArgumentException("Could not infer file type"), req("/api/import/file"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("INVALID_REQUEST", resp.getBody().code());
    }

    @Test
    void handleUnexpected_shouldReturn500_withoutLeakingDetails() {
        ResponseEntity<ApiErrorResponse> resp =
                handler.handleUnexpected(new RuntimeException("NPE at internal.Secret.line42"), req("/api/import/file"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("INTERNAL_ERROR", resp.getBody().code());
        assertEquals("An unexpected error occurred", resp.getBody().message());
        assertFalse(resp.getBody().message().contains("Secret"));
    }
}
