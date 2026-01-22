package br.com.ecofy.ms_notification.core.domain;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationStatus;
import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.domain.valueobject.IdempotencyKey;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Getter
@Builder(toBuilder = true)
public class Notification {

    private final NotificationId id;
    private final UserId userId;

    private final DomainEventType eventType;
    private final NotificationChannel channel;

    private final ChannelAddress destination;
    private final String subject;
    private final String body;

    private final NotificationStatus status;
    private final int attemptCount;

    private final IdempotencyKey idempotencyKey;

    private final Map<String, Object> payload;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Notification validateForSend() {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(body, "body must not be null");
        if (body.isBlank()) throw new IllegalArgumentException("body must not be blank");

        if (channel == NotificationChannel.EMAIL) {
            if (subject == null || subject.isBlank()) {
                throw new IllegalArgumentException("subject must not be blank for EMAIL");
            }
        }
        return this;
    }

}