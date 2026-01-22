package br.com.ecofy.ms_insights.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.Map;

public record InsightCreatedEvent(

        String eventId,

        String type,

        String userId,

        String insightId,

        int score,

        Instant createdAt,

        Map<String, Object> payload

) { }
