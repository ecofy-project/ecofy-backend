package br.com.ecofy.ms_notification.adapters.in.web.dto.response;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(

        UUID id,

        UUID userId,

        DomainEventType eventType,

        NotificationChannel channel,

        String destination,

        String subject,

        String body,

        NotificationStatus status,

        int attemptCount,

        Map<String, Object> payload,

        Instant createdAt,

        Instant updatedAt

) { }
