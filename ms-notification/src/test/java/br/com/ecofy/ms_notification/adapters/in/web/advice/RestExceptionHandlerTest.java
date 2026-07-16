package br.com.ecofy.ms_notification.adapters.in.web.advice;

import br.com.ecofy.ms_notification.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_notification.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_notification.core.domain.exception.NotificationNotFoundException;
import br.com.ecofy.ms_notification.core.domain.exception.TemplateNotFoundException;
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
        when(r.getRequestURI()).thenReturn("/api/notification/v1/notifications");
        return r;
    }

    @Test
    void handleGeneric_shouldReturn500WithGenericMessage_notLeakingInternalDetails() {
        var ex = new RuntimeException("jdbc: connection refused to mongo at 10.0.0.5:27017 (secret-detail)");

        ResponseEntity<ApiErrorResponse> resp = handler.handleGeneric(ex, req());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
        // Não vaza a mensagem interna da exceção
        assertThat(resp.getBody().message()).isEqualTo("Erro interno inesperado ao processar a notificação.");
        assertThat(resp.getBody().message()).doesNotContain("mongo", "jdbc", "secret-detail");
    }

    @Test
    void handleTemplateNotFound_shouldReturn404() {
        var resp = handler.handleTemplateNotFound(new TemplateNotFoundException("nope"), req());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().errorCode()).isEqualTo("TEMPLATE_NOT_FOUND");
    }

    @Test
    void handleNotificationNotFound_shouldReturn404() {
        var resp = handler.handleNotificationNotFound(new NotificationNotFoundException("nope"), req());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleIdempotency_shouldReturn409() {
        var resp = handler.handleIdempotency(new IdempotencyViolationException("dup"), req());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().errorCode()).isEqualTo("IDEMPOTENCY_VIOLATION");
    }

    @Test
    void handleBusiness_shouldReturn400() {
        var resp = handler.handleBusiness(new BusinessValidationException("bad"), req());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().errorCode()).isEqualTo("BUSINESS_VALIDATION");
    }
}
