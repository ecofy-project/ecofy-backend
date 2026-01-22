package br.com.ecofy.ms_insights.adapters.in.web.dto.response;

import java.util.List;

public record InsightsBundleResponse(

        List<InsightResponse> insights,

        List<Object> metrics,

        List<GoalResponse> goals

) {}
