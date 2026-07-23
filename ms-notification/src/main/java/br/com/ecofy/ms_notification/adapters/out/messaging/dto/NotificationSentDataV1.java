package br.com.ecofy.ms_notification.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.UUID;

// Transporta os dados de auditoria de entrega confirmada, sem conteúdo da mensagem nem contatos.
public record NotificationSentDataV1(
        UUID notificationId,
        UUID userId,
        String channel,
        String provider,
        Instant deliveredAt
) {
}
