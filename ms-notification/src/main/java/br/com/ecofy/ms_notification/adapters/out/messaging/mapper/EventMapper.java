package br.com.ecofy.ms_notification.adapters.out.messaging.mapper;

import br.com.ecofy.ms_notification.adapters.out.messaging.dto.NotificationSentEvent;
import br.com.ecofy.ms_notification.core.domain.Notification;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EventMapper {

    /**
     * Overload recomendado: quem chama não precisa se preocupar com ids.
     * Gera publishId (runId) e um eventId padrão.
     */
    public NotificationSentEvent toSentEvent(Notification notification) {
        UUID publishId = UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        return toSentEvent(notification, publishId, eventId, null);
    }

    /**
     * Overload intermediário: permite passar correlationId.
     * Útil quando existe trace/correlation vindo do fluxo anterior (ex.: Kafka inbound).
     */
    public NotificationSentEvent toSentEvent(
            Notification notification,
            String correlationId
    ) {
        UUID publishId = UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        return toSentEvent(notification, publishId, eventId, correlationId);
    }

    /**
     * Overload completo: permite controlar publishId/eventId/correlationId explicitamente.
     * Deixa o mapper pronto para uma futura evolução do DTO com metadados.
     */
    public NotificationSentEvent toSentEvent(
            Notification notification,
            UUID publishId,
            String eventId,
            String correlationId
    ) {
        if (notification == null) throw new IllegalArgumentException("notification must not be null");
        if (publishId == null) throw new IllegalArgumentException("publishId must not be null");

        // Base event (estado atual do seu DTO)
        NotificationSentEvent base = NotificationSentEvent.from(notification);

        // Se o seu NotificationSentEvent não possui metadados hoje, retornamos o base.
        // Caso você adicione metadados no DTO no futuro, implemente aqui algo como:
        // return base.withMetadata(new Metadata(publishId, blankToNull(eventId), blankToNull(correlationId)));
        //
        // Enquanto não existir suporte no DTO, mantemos a assinatura do mapper pronta
        // e normalizamos os valores para evitar lixo caso você use em logs.
        blankToNull(eventId);
        blankToNull(correlationId);

        return base;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
