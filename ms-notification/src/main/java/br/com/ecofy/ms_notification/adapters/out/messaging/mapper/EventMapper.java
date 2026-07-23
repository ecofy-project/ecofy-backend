package br.com.ecofy.ms_notification.adapters.out.messaging.mapper;

import br.com.ecofy.ms_notification.adapters.out.messaging.dto.NotificationSentEvent;
import br.com.ecofy.ms_notification.core.domain.Notification;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Converte notificações do domínio em eventos de mensageria.
@Component
public class EventMapper {

    public NotificationSentEvent toSentEvent(Notification notification) {
        UUID publishId = UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        return toSentEvent(notification, publishId, eventId, null);
    }

    public NotificationSentEvent toSentEvent(
            Notification notification,
            String correlationId
    ) {
        UUID publishId = UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        return toSentEvent(notification, publishId, eventId, correlationId);
    }

    // Valida os dados obrigatórios antes de converter a notificação em evento.
    public NotificationSentEvent toSentEvent(
            Notification notification,
            UUID publishId,
            String eventId,
            String correlationId
    ) {
        if (notification == null) {
            throw new IllegalArgumentException("notification must not be null");
        }
        if (publishId == null) {
            throw new IllegalArgumentException("publishId must not be null");
        }

        NotificationSentEvent base = NotificationSentEvent.from(notification);

        blankToNull(eventId);
        blankToNull(correlationId);

        return base;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
