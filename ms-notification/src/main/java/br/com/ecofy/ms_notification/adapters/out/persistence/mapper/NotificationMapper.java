package br.com.ecofy.ms_notification.adapters.out.persistence.mapper;

import br.com.ecofy.ms_notification.adapters.out.persistence.document.NotificationDocument;
import br.com.ecofy.ms_notification.core.domain.Notification;
import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.domain.valueobject.IdempotencyKey;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class NotificationMapper {

    // Converte o domínio Notification em NotificationDocument (Mongo), validando campos obrigatórios e normalizando strings/opcionais.
    public NotificationDocument toDoc(Notification n) {
        if (n == null) throw new IllegalArgumentException("notification must not be null");

        UUID id = requireId(n);
        UUID userId = requireUserId(n);
        String destination = requireDestination(n);

        return NotificationDocument.builder()
                .id(id)
                .userId(userId)
                .eventType(n.getEventType())
                .channel(n.getChannel())
                .destination(destination)
                .subject(blankToNull(n.getSubject()))
                .body(blankToNull(n.getBody()))
                .status(n.getStatus())
                .attemptCount(n.getAttemptCount())
                .idempotencyKey(toIdempotencyKeyValue(n.getIdempotencyKey()))
                .payload(n.getPayload())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }

    // Converte NotificationDocument (Mongo) em Notification (domínio), reconstruindo value objects e normalizando strings.
    public Notification toDomain(NotificationDocument d) {
        if (d == null) throw new IllegalArgumentException("document must not be null");

        UUID id = Objects.requireNonNull(d.getId(), "document.id must not be null");
        UUID userId = Objects.requireNonNull(d.getUserId(), "document.userId must not be null");
        String destination = Objects.requireNonNull(d.getDestination(), "document.destination must not be null");

        return Notification.builder()
                .id(new NotificationId(id))
                .userId(new UserId(userId))
                .eventType(d.getEventType())
                .channel(d.getChannel())
                .destination(new ChannelAddress(d.getChannel(), destination))
                .subject(blankToNull(d.getSubject()))
                .body(blankToNull(d.getBody()))
                .status(d.getStatus())
                .attemptCount(d.getAttemptCount())
                .idempotencyKey(toIdempotencyKey(d.getIdempotencyKey()))
                .payload(d.getPayload())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    // Extrai e valida o UUID do NotificationId no domínio, falhando rápido se id/value estiver ausente.
    private static UUID requireId(Notification n) {
        if (n.getId() == null) throw new IllegalArgumentException("notification.id must not be null");
        UUID id = n.getId().value();
        if (id == null) throw new IllegalArgumentException("notification.id.value must not be null");
        return id;
    }

    // Extrai e valida o UUID do UserId no domínio, falhando rápido se userId/value estiver ausente.
    private static UUID requireUserId(Notification n) {
        if (n.getUserId() == null) throw new IllegalArgumentException("notification.userId must not be null");
        UUID userId = n.getUserId().value();
        if (userId == null) throw new IllegalArgumentException("notification.userId.value must not be null");
        return userId;
    }

    // Extrai e valida o endereço de destino do domínio (ChannelAddress.address), garantindo que não seja vazio e retornando trimmed.
    private static String requireDestination(Notification n) {
        if (n.getDestination() == null) throw new IllegalArgumentException("notification.destination must not be null");
        String dest = n.getDestination().address();
        if (dest == null || dest.isBlank()) throw new IllegalArgumentException("notification.destination.address must not be blank");
        return dest.trim();
    }

    // Converte IdempotencyKey (value object) para String persistível, normalizando vazio/em branco para null.
    private static String toIdempotencyKeyValue(IdempotencyKey key) {
        if (key == null) return null;
        String v = key.value();
        return blankToNull(v);
    }

    // Converte String persistida em IdempotencyKey (value object), retornando null se a string estiver vazia/em branco.
    private static IdempotencyKey toIdempotencyKey(String v) {
        String safe = blankToNull(v);
        return safe == null ? null : new IdempotencyKey(safe);
    }

    // Normaliza strings removendo espaços e convertendo valores nulos/em branco para null.
    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

}
