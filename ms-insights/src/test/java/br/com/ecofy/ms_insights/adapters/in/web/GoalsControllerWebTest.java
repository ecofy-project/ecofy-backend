package br.com.ecofy.ms_insights.adapters.in.web;

import br.com.ecofy.ms_insights.adapters.in.web.advice.RestExceptionHandler;
import br.com.ecofy.ms_insights.core.application.result.GoalResult;
import br.com.ecofy.ms_insights.core.application.service.GoalService;
import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;
import br.com.ecofy.ms_insights.core.domain.exception.GoalNotFoundException;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GoalsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class GoalsControllerWebTest {

    @Autowired
    private MockMvc mvc;

    // GoalService implementa UpdateGoalUseCase/ListGoalsUseCase/GetGoalUseCase, então um único mock
    // satisfaz os quatro parâmetros do controller (evita ambiguidade de beans).
    @MockitoBean
    private GoalService goalService;

    private GoalResult goal(UUID userId) {
        var now = Instant.now();
        return new GoalResult(UUID.randomUUID(), userId, "Trip", 100_000L, "BRL", GoalStatus.ACTIVE, now, now);
    }

    @Test
    void create_shouldReturn201() throws Exception {
        UUID userId = UUID.randomUUID();
        when(goalService.create(any())).thenReturn(goal(userId));

        String body = """
                {"userId":"%s","name":"Trip","targetCents":100000,"currency":"BRL","status":"ACTIVE"}
                """.formatted(userId);

        mvc.perform(post("/api/insights/v1/goals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void create_shouldReturn400_whenTargetNotPositive() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = """
                {"userId":"%s","name":"Trip","targetCents":0,"currency":"BRL"}
                """.formatted(userId);

        mvc.perform(post("/api/insights/v1/goals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400_whenCurrencyInvalidLength() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = """
                {"userId":"%s","name":"Trip","targetCents":100,"currency":"REAIS"}
                """.formatted(userId);

        mvc.perform(post("/api/insights/v1/goals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_shouldReturn404_whenGoalMissing() throws Exception {
        when(goalService.get(any())).thenThrow(new GoalNotFoundException("nope"));

        mvc.perform(get("/api/insights/v1/goals/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listByUser_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(goalService.list(any())).thenReturn(List.of(goal(userId)));

        mvc.perform(get("/api/insights/v1/goals").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }
}
