package br.com.ecofy.ms_insights.config;

import br.com.ecofy.ms_insights.adapters.in.web.GoalsController;
import br.com.ecofy.ms_insights.core.application.result.GoalResult;
import br.com.ecofy.ms_insights.core.application.service.GoalService;
import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Correção Dia 8 (item #1): com permit-all=false (prod-like), /api/insights/** exige JWT
 * (401 sem token; acessível com token válido).
 */
@WebMvcTest(controllers = GoalsController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "ecofy.insights.security.permit-all=false")
class SecurityConfigWebTest {

    @Autowired
    private MockMvc mvc;

    // GoalService satisfaz os quatro parâmetros do controller (evita ambiguidade de beans).
    @MockitoBean
    private GoalService goalService;
    @MockitoBean
    private JwtDecoder jwtDecoder; // exigido por oauth2ResourceServer.jwt()

    @Test
    void protectedEndpoint_shouldReturn401_withoutJwt() throws Exception {
        mvc.perform(get("/api/insights/v1/goals").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldReturn200_withValidJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        var now = Instant.now();
        when(goalService.list(any())).thenReturn(List.of(
                new GoalResult(UUID.randomUUID(), userId, "Trip", 100L, "BRL", GoalStatus.ACTIVE, now, now)));

        mvc.perform(get("/api/insights/v1/goals").param("userId", userId.toString()).with(jwt()))
                .andExpect(status().isOk());
    }
}
