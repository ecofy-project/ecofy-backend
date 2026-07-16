package br.com.ecofy.ms_notification.config;

import br.com.ecofy.ms_notification.adapters.in.web.NotificationController;
import br.com.ecofy.ms_notification.core.application.result.NotificationResult;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationStatus;
import br.com.ecofy.ms_notification.core.port.in.ListNotificationsUseCase;
import br.com.ecofy.ms_notification.core.port.in.ResendNotificationUseCase;
import br.com.ecofy.ms_notification.core.port.in.SendNotificationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Correção Dia 7 (item #2): segurança por profile. Com permit-all=false (prod-like),
 * /api/notification/** exige JWT (401 sem token; acessível com token válido).
 */
@WebMvcTest(controllers = NotificationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "ecofy.notification.security.permit-all=false")
class SecurityConfigWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SendNotificationUseCase sendUseCase;
    @MockitoBean
    private ResendNotificationUseCase resendUseCase;
    @MockitoBean
    private ListNotificationsUseCase listUseCase;
    @MockitoBean
    private JwtDecoder jwtDecoder; // exigido por oauth2ResourceServer.jwt()

    @Test
    void protectedEndpoint_shouldReturn401_withoutJwt() throws Exception {
        mvc.perform(get("/api/notification/v1/notifications").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldReturn200_withValidJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        var now = Instant.now();
        when(listUseCase.listByUser(eq(userId), anyInt())).thenReturn(List.of(
                new NotificationResult(UUID.randomUUID(), userId, DomainEventType.BUDGET_ALERT,
                        NotificationChannel.EMAIL, "user@example.com", "s", "b",
                        NotificationStatus.SENT, 1, Map.of(), now, now)));

        mvc.perform(get("/api/notification/v1/notifications")
                        .param("userId", userId.toString())
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void publicActuatorHealth_shouldBeAccessibleWithoutJwt() throws Exception {
        // health é público; pode retornar 200 ou 404 (endpoint não montado no slice), mas nunca 401.
        int statusCode = mvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();
        assert statusCode != 401;
    }
}
