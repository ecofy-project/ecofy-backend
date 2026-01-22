package br.com.ecofy.ms_notification.core.application.result;

import br.com.ecofy.ms_notification.core.domain.enums.AttemptStatus;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAttemptResult(

        UUID id,

        UUID notificationId,

        NotificationChannel channel,

        int attemptNumber,

        AttemptStatus status,

        String provider,

        String providerMessageId,

        String errorCode,

        String errorMessage,

        Instant createdAt

) { }