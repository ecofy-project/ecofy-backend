package br.com.ecofy.ms_insights.adapters.in.web.dto.request;

import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;

public record UpdateGoalRequest(

        String name,

        Long targetCents,

        String currency,

        GoalStatus status

) { }
