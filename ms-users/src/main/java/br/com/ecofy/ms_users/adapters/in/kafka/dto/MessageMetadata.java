package br.com.ecofy.ms_users.adapters.in.kafka.dto;

import java.time.Instant;

public record MessageMetadata(

        String eventId,

        Instant occurredAt,

        String traceId,

        String source

) { }
