package br.com.ecofy.auth.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.UUID;

// Representa os dados publicados após o registro de um usuário.
public record AuthUserRegisteredMessage(
        UUID userId,
        String externalAuthId,
        String fullName,
        String email,
        String phone,
        EventMetadata metadata
) {

    // Agrupa os metadados utilizados no rastreamento do evento.
    public record EventMetadata(
            String eventId,
            Instant occurredAt,
            String traceId,
            String source
    ) {}
}
