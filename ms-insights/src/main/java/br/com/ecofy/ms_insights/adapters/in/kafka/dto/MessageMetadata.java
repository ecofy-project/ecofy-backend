package br.com.ecofy.ms_insights.adapters.in.kafka.dto;

import java.time.Instant;

public record MessageMetadata(

        String messageId,

        String correlationId,

        Instant producedAt

) { }
