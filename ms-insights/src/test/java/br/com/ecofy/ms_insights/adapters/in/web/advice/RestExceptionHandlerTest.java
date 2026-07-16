package br.com.ecofy.ms_insights.adapters.in.web.advice;

import br.com.ecofy.ms_insights.core.domain.exception.ExternalDataUnavailableException;
import br.com.ecofy.ms_insights.core.domain.exception.GoalNotFoundException;
import br.com.ecofy.ms_insights.core.domain.exception.IdempotencyViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    private HttpServletRequest req() {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn("/api/insights/v1/generate");
        return r;
    }

    @Test
    void generic_shouldReturn500WithoutLeakingInternalMessage() {
        var ex = new RuntimeException("jdbc: connection refused to postgres 10.0.0.9 (secret)");
        ResponseEntity<ApiErrorResponse> resp = handler.generic(ex, req());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().message()).isEqualTo("Unexpected error");
        assertThat(resp.getBody().message()).doesNotContain("postgres", "jdbc", "secret");
    }

    @Test
    void goalNotFound_shouldReturn404() {
        var resp = handler.goalNotFound(new GoalNotFoundException("nope"), req());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().details()).containsEntry("reason", "GOAL_NOT_FOUND");
    }

    @Test
    void idempotency_shouldReturn409() {
        var resp = handler.idem(new IdempotencyViolationException("dup"), req());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void illegalArgument_shouldReturn400() {
        var resp = handler.illegalArgument(new IllegalArgumentException("end must be >= start"), req());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().details()).containsEntry("reason", "INVALID_ARGUMENT");
    }

    @Test
    void externalUnavailable_shouldReturn503WithSource() {
        var resp = handler.externalUnavailable(
                new ExternalDataUnavailableException("budgeting", "down", new RuntimeException()), req());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody().details()).containsEntry("source", "budgeting");
    }
}
