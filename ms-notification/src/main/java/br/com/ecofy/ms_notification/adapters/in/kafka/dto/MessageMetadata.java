package br.com.ecofy.ms_notification.adapters.in.kafka.dto;

import java.time.Instant;
import java.util.Map;

public record MessageMetadata(

        String eventId,

        String correlationId,

        Instant occurredAt,

        String source

) {

    public static MessageMetadata minimal() {
        return new MessageMetadata(null, null, Instant.now(), "kafka");
    }

}
