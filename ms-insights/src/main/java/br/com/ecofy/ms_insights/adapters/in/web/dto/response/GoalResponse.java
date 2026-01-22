package br.com.ecofy.ms_insights.adapters.in.web.dto.response;

import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;

import java.time.Instant;
import java.util.UUID;

public record GoalResponse(

        UUID id,

        UUID userId,

        String name,

        long targetCents,

        String currency,

        GoalStatus status,

        Instant createdAt,

        Instant updatedAt

) { }
