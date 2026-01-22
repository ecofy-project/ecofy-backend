package br.com.ecofy.ms_insights.adapters.out.messaging.dto;

import java.time.Instant;

public record ReportReadyEvent(

        String eventId,

        String userId,

        String reportId,

        String artifactRef,

        Instant createdAt

) { }
