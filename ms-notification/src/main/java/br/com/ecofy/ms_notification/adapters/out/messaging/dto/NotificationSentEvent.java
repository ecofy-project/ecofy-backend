package br.com.ecofy.ms_notification.adapters.out.messaging.dto;

import br.com.ecofy.ms_notification.core.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationSentEvent(
        UUID notificationId,
        UUID userId,
        String eventType,
        String channel,
        String status,
        Instant occurredAt
) {
    public static NotificationSentEvent from(Notification n) {
        return new NotificationSentEvent(
                n.getId().value(),
                n.getUserId().value(),
                n.getEventType().name(),
                n.getChannel().name(),
                n.getStatus().name(),
                Instant.now()
        );
    }
}
