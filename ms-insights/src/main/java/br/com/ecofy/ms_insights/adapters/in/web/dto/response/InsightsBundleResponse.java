package br.com.ecofy.ms_insights.adapters.in.web.dto.response;

import java.util.List;

/**
 * Correção Dia 8 (item #3): {@code metrics} passou de {@code List<Object>} (com aninhamento
 * artificial) para {@code List<MetricSnapshotResponse>} tipado — sem lista aninhada, contrato claro.
 */
public record InsightsBundleResponse(

        List<InsightResponse> insights,

        List<MetricSnapshotResponse> metrics,

        List<GoalResponse> goals

) {}
