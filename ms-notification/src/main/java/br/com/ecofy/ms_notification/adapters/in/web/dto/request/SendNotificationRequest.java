package br.com.ecofy.ms_notification.adapters.in.web.dto.request;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(

        @NotNull UUID userId,

        @NotNull DomainEventType eventType,

        @NotNull NotificationChannel channel,

        String destinationOverride,

        Map<String, Object> payload,

        String idempotencyKey

) { }
