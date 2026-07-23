package br.com.ecofy.ms_insights.adapters.in.web.dto.response;

import java.util.List;

// Agrupa insights, métricas e metas do dashboard em um contrato tipado.
public record InsightsBundleResponse(

        List<InsightResponse> insights,

        List<MetricSnapshotResponse> metrics,

        List<GoalResponse> goals

) {}
