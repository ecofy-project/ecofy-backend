package br.com.ecofy.ms_notification.adapters.in.kafka.dto;

import java.util.UUID;

public record InsightCreatedEventMessage(

        UUID userId,

        UUID insightId,

        String insightType,

        String periodStart,

        String periodEnd,

        MessageMetadata metadata

) { }