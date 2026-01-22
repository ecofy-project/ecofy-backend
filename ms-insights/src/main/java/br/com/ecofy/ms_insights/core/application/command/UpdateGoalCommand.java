package br.com.ecofy.ms_insights.core.application.command;

import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;

import java.util.UUID;

public record UpdateGoalCommand(

        UUID goalId,

        String name,

        Long targetCents,

        String currency,

        GoalStatus status

) { }
