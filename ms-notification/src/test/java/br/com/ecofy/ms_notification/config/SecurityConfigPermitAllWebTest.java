package br.com.ecofy.ms_notification.config;

import br.com.ecofy.ms_notification.adapters.in.web.NotificationController;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Correção Dia 7 (item #2): em dev/test/local (permit-all=true), /api/notification/** é acessível
 * sem JWT (comportamento documentado para facilitar testes locais).
 */
@WebMvcTest(controllers = NotificationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "ecofy.notification.security.permit-all=true")
class SecurityConfigPermitAllWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SendNotificationUseCase sendUseCase;
    @MockitoBean
    private ResendNotificationUseCase resendUseCase;
    @MockitoBean
    private ListNotificationsUseCase listUseCase;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void endpoint_shouldBeAccessibleWithoutJwt_whenPermitAll() throws Exception {
        when(listUseCase.listByUser(any(), anyInt())).thenReturn(List.of());

        mvc.perform(get("/api/notification/v1/notifications").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }
}
