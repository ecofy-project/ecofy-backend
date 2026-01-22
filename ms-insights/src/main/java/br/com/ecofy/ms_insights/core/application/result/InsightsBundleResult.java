package br.com.ecofy.ms_insights.core.application.result;

import java.util.List;

public record InsightsBundleResult(

        List<InsightResult> insights,

        List<MetricSnapshotResult> metrics,

        List<GoalResult> goals

) { }
