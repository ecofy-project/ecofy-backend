package br.com.ecofy.ms_categorization.adapters.in.kafka.dto;

import java.time.Instant;

public record MessageMetadata(

        String messageId,

        Instant producedAt,

        String producer,

        String traceId

) { }