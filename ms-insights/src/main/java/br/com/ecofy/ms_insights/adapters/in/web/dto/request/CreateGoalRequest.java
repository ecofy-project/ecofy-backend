package br.com.ecofy.ms_insights.adapters.in.web.dto.request;

import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;

public record CreateGoalRequest(

        java.util.UUID userId,

        String name,

        long targetCents,

        String currency,

        GoalStatus status

) { }
