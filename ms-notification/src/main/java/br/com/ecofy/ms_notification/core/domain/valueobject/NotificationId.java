package br.com.ecofy.ms_notification.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record NotificationId(UUID value) {

    // Garante que o identificador da notificação seja válido (não nulo) ao criar o value object.
    public NotificationId {
        Objects.requireNonNull(value, "notificationId must not be null");
    }

    // Gera um novo NotificationId com UUID aleatório para uso na criação de novas notificações.
    public static NotificationId newId() {
        return new NotificationId(UUID.randomUUID());
    }

}
