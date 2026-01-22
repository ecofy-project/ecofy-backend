package br.com.ecofy.ms_insights.adapters.in.web.dto.response;

import br.com.ecofy.ms_insights.core.domain.enums.InsightType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record InsightResponse(

        UUID id,

        UUID userId,

        InsightType type,

        int score,

        String title,

        String summary,

        Map<String, Object> payload,

        Instant createdAt

) { }
