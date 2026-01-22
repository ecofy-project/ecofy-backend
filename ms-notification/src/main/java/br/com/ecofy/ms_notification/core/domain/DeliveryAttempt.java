package br.com.ecofy.ms_notification.core.domain;


import br.com.ecofy.ms_notification.core.domain.enums.AttemptStatus;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class DeliveryAttempt {

    private final UUID id; // attempt id
    private final NotificationId notificationId;
    private final NotificationChannel channel;

    private final int attemptNumber;
    private final AttemptStatus status;

    private final String provider;
    private final String providerMessageId;

    private final String errorCode;
    private final String errorMessage;

    private final Instant createdAt;

    // Factory method para criar uma tentativa de entrega bem-sucedida com dados do provedor.
    public static DeliveryAttempt success(
            NotificationId notificationId,
            NotificationChannel channel,
            int attemptNumber,
            String provider,
            String providerMessageId
    ) {
        return DeliveryAttempt.builder()
                .id(UUID.randomUUID())
                .notificationId(notificationId)
                .channel(channel)
                .attemptNumber(attemptNumber)
                .status(AttemptStatus.SUCCESS)
                .provider(provider)
                .providerMessageId(providerMessageId)
                .createdAt(Instant.now())
                .build();
    }

    // Factory method para criar uma tentativa de entrega com falha, registrando código e mensagem de erro.
    public static DeliveryAttempt failure(
            NotificationId notificationId,
            NotificationChannel channel,
            int attemptNumber,
            String provider,
            String errorCode,
            String errorMessage
    ) {
        return DeliveryAttempt.builder()
                .id(UUID.randomUUID())
                .notificationId(notificationId)
                .channel(channel)
                .attemptNumber(attemptNumber)
                .status(AttemptStatus.FAILED)
                .provider(provider)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .createdAt(Instant.now())
                .build();
    }
}