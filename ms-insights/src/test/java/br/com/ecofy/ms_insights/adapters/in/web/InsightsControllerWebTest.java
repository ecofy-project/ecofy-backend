package br.com.ecofy.ms_insights.adapters.in.web;

import br.com.ecofy.ms_insights.adapters.in.web.advice.RestExceptionHandler;
import br.com.ecofy.ms_insights.core.application.result.InsightsBundleResult;
import br.com.ecofy.ms_insights.core.application.result.MetricSnapshotResult;
import br.com.ecofy.ms_insights.core.domain.enums.MetricType;
import br.com.ecofy.ms_insights.core.port.in.GenerateInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.in.GetDashboardInsightsUseCase;
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

/**
 * Correção Dia 8 (item #3/#12): valida que metrics é uma lista TIPADA e PLANA
 * (sem List&lt;Object&gt; nem aninhamento artificial).
 */
@WebMvcTest(controllers = InsightsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class InsightsControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GenerateInsightsUseCase generateInsightsUseCase;
    @MockitoBean
    private GetDashboardInsightsUseCase getDashboardInsightsUseCase;

    private InsightsBundleResult bundleWithMetric(UUID userId) {
        var metric = new MetricSnapshotResult(UUID.randomUUID(), userId, MetricType.TOTAL_SPENT, 1500L, "BRL", Instant.now());
        return new InsightsBundleResult(List.of(), List.of(metric), List.of());
    }

    @Test
    void dashboard_shouldReturnFlatTypedMetrics_notNested() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getDashboardInsightsUseCase.getDashboard(any())).thenReturn(bundleWithMetric(userId));

        mvc.perform(get("/api/insights/v1/dashboard/{userId}", userId))
                .andExpect(status().isOk())
                // metrics[0] é um OBJETO com campos (não um array aninhado)
                .andExpect(jsonPath("$.metrics[0].metricType").value("TOTAL_SPENT"))
                .andExpect(jsonPath("$.metrics[0].valueCents").value(1500))
                .andExpect(jsonPath("$.metrics[0].currency").value("BRL"))
                .andExpect(jsonPath("$.metrics[0].userId").value(userId.toString()))
                // garante que NÃO é lista aninhada: metrics[0] não é array
                .andExpect(jsonPath("$.metrics[0][0]").doesNotExist());
    }

    @Test
    void generate_shouldReturn400_whenBodyMissingRequiredFields() throws Exception {
        String body = "{\"userId\":null}";
        mvc.perform(post("/api/insights/v1/generate")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
