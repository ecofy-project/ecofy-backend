package br.com.ecofy.ms_budgeting.adapters.in.kafka.dto;

import java.time.Instant;

public record MessageMetadata (

        String eventId,

        Instant occurredAt,

        String producer,

        String traceId

) { }