package br.com.ecofy.ms_notification.adapters.in.web;

import br.com.ecofy.ms_notification.adapters.in.web.advice.RestExceptionHandler;
import br.com.ecofy.ms_notification.core.application.result.NotificationResult;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationStatus;
import br.com.ecofy.ms_notification.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_notification.core.port.in.ListNotificationsUseCase;
import br.com.ecofy.ms_notification.core.port.in.ResendNotificationUseCase;
import br.com.ecofy.ms_notification.core.port.in.SendNotificationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Correção Dia 7 (item #3): testes web do NotificationController (contratos 201/400/409/200).
 * Security desativada no slice (addFilters=false) para focar no comportamento do controller;
 * a segurança por profile é testada em SecurityConfigWebTest.
 */
@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class NotificationControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SendNotificationUseCase sendUseCase;
    @MockitoBean
    private ResendNotificationUseCase resendUseCase;
    @MockitoBean
    private ListNotificationsUseCase listUseCase;

    private NotificationResult result(UUID userId) {
        var now = Instant.now();
        return new NotificationResult(UUID.randomUUID(), userId, DomainEventType.BUDGET_ALERT,
                NotificationChannel.EMAIL, "user@example.com", "s", "b",
                NotificationStatus.SENT, 1, Map.of(), now, now);
    }

    @Test
    void send_shouldReturn201() throws Exception {
        UUID userId = UUID.randomUUID();
        when(sendUseCase.send(any())).thenReturn(result(userId));

        String body = """
                {"userId":"%s","eventType":"BUDGET_ALERT","channel":"EMAIL","destinationOverride":"user@example.com","payload":{}}
                """.formatted(userId);

        mvc.perform(post("/api/notification/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void send_shouldReturn400_whenUserIdMissing() throws Exception {
        String body = """
                {"eventType":"BUDGET_ALERT","channel":"EMAIL"}
                """;

        mvc.perform(post("/api/notification/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void send_shouldReturn409_onIdempotencyViolation() throws Exception {
        UUID userId = UUID.randomUUID();
        when(sendUseCase.send(any())).thenThrow(new IdempotencyViolationException("dup"));

        String body = """
                {"userId":"%s","eventType":"BUDGET_ALERT","channel":"EMAIL"}
                """.formatted(userId);

        mvc.perform(post("/api/notification/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_VIOLATION"));
    }

    @Test
    void list_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(listUseCase.listByUser(eq(userId), anyInt())).thenReturn(List.of(result(userId)));

        mvc.perform(get("/api/notification/v1/notifications").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }
}
