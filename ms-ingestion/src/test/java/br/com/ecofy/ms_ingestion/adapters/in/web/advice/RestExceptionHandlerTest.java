package br.com.ecofy.ms_ingestion.adapters.in.web.advice;

import br.com.ecofy.ms_ingestion.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.ErrorDetail;
import br.com.ecofy.ms_ingestion.core.application.exception.IdempotencyKeyPayloadMismatchException;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportAccessForbiddenException;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportAlreadyProcessedException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionErrorCode;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import br.com.ecofy.ms_ingestion.core.application.exception.InvalidFileHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler(new IngestionProperties());

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private HttpServletRequest req(String uri) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn(uri);
        return r;
    }

    // ---- contrato do ApiErrorResponse (§20.9) ----

    @Test
    void response_bodyStatusAlwaysMatchesHttpStatus() {
        var ex = new IngestionException(IngestionErrorCode.IMPORT_JOB_NOT_FOUND, "ImportJob not found");

        ResponseEntity<ApiErrorResponse> resp = handler.handleIngestion(ex, req("/api/import/jobs/x"));

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals(resp.getStatusCode().value(), resp.getBody().status());
        assertEquals("IMPORT_JOB_NOT_FOUND", resp.getBody().errorCode());
        assertEquals("/api/import/jobs/x", resp.getBody().path());
    }

    // Verifica que os detalhes do erro nunca vêm nulos na resposta.
    @Test
    void response_detailsIsNeverNull() {
        var ex = new IngestionException(IngestionErrorCode.IMPORT_JOB_NOT_FOUND, "not found");

        ResponseEntity<ApiErrorResponse> resp = handler.handleIngestion(ex, req("/api/import/jobs/x"));

        assertNotNull(resp.getBody().details());
        assertTrue(resp.getBody().details().isEmpty());
    }

    @Test
    void response_timestampIsPresentAndUtc() {
        var ex = new IngestionException(IngestionErrorCode.IMPORT_JOB_NOT_FOUND, "not found");

        ApiErrorResponse body = handler.handleIngestion(ex, req("/api/import/jobs/x")).getBody();

        assertNotNull(body.timestamp());
        // Instant é UTC por definição e serializa com sufixo Z.
        assertTrue(body.timestamp().toString().endsWith("Z"));
    }

    @Test
    void response_traceIdComesFromCorrelationIdMdc() {
        MDC.put(CorrelationId.MDC_KEY, "corr-abc-123");

        var ex = new IngestionException(IngestionErrorCode.IMPORT_JOB_NOT_FOUND, "not found");
        ApiErrorResponse body = handler.handleIngestion(ex, req("/api/import/jobs/x")).getBody();

        assertEquals("corr-abc-123", body.traceId());
    }

    // ---- status por código ----

    @Test
    void maxUploadSizeExceeded_returns413WithAllowedLimit() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleMaxUpload(
                new MaxUploadSizeExceededException(10), req("/api/import/file"));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, resp.getStatusCode());
        assertEquals("FILE_SIZE_LIMIT_EXCEEDED", resp.getBody().errorCode());
        // O limite permitido é devolvido para o frontend orientar o usuário (§6.3).
        assertTrue(resp.getBody().details().stream().anyMatch(d -> "maxFileSizeBytes".equals(d.field())));
    }

    @Test
    void alreadyProcessed_returns409PointingToExistingJob() {
        UUID existingJobId = UUID.randomUUID();

        ResponseEntity<ApiErrorResponse> resp = handler.handleAlreadyProcessed(
                new ImportAlreadyProcessedException(existingJobId, "abc123"), req("/api/import/file"));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("IMPORT_ALREADY_PROCESSED", resp.getBody().errorCode());
        assertEquals("/api/import/jobs/" + existingJobId, resp.getHeaders().getFirst("Location"));
    }

    @Test
    void idempotencyKeyMismatch_returns409() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleIdempotencyMismatch(
                new IdempotencyKeyPayloadMismatchException("key-1"), req("/api/import/file"));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH", resp.getBody().errorCode());
    }

    @Test
    void accessForbidden_returns403WithoutRevealingJobExistence() {
        UUID jobId = UUID.randomUUID();

        ResponseEntity<ApiErrorResponse> resp = handler.handleForbidden(
                new ImportAccessForbiddenException(jobId), req("/api/import/jobs/" + jobId));

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        // A mensagem não confirma que o job existe nem quem é o dono.
        assertFalse(resp.getBody().message().contains(jobId.toString()));
        assertTrue(resp.getBody().details().isEmpty());
    }

    @Test
    void invalidHeader_returns422WithColumnDetails() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleInvalidHeader(
                new InvalidFileHeaderException(List.of(
                        ErrorDetail.ofField("header", "MISSING_REQUIRED_COLUMN", "A coluna date e obrigatoria."))),
                req("/api/import/file"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("INVALID_FILE_HEADER", resp.getBody().errorCode());
        assertEquals(1, resp.getBody().details().size());
        assertEquals("MISSING_REQUIRED_COLUMN", resp.getBody().details().get(0).code());
    }

    // ---- não vazar detalhe interno (§17.3) ----

    @Test
    void ingestionError_doesNotLeakLocalFilePath() {
        var ex = new IngestionException(
                IngestionErrorCode.STORAGE_ERROR, "Failed writing C:\\var\\lib\\ecofy\\secret.csv");

        ApiErrorResponse body = handler.handleIngestion(ex, req("/api/import/file")).getBody();

        assertFalse(body.message().contains("secret.csv"));
        assertFalse(body.message().contains("C:\\"));
    }

    @Test
    void unexpectedError_returns500WithoutLeakingDetails() {
        ResponseEntity<ApiErrorResponse> resp = handler.handleUnexpected(
                new RuntimeException("NPE at internal.Secret.line42"), req("/api/import/file"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("INTERNAL_INGESTION_ERROR", resp.getBody().errorCode());
        assertFalse(resp.getBody().message().contains("Secret"));
    }
}
