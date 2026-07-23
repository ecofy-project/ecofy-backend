package br.com.ecofy.ms_notification.adapters.out.messaging;

import br.com.ecofy.ms_notification.adapters.out.messaging.dto.EventEnvelope;
import br.com.ecofy.ms_notification.adapters.out.messaging.dto.NotificationSentDataV1;
import br.com.ecofy.ms_notification.adapters.out.persistence.document.NotificationOutboxDocument;
import br.com.ecofy.ms_notification.adapters.out.persistence.repository.NotificationOutboxRepository;
import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.domain.Notification;
import br.com.ecofy.ms_notification.core.port.out.PublishNotificationEventPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Grava o evento de entrega confirmada na Outbox, com eventId estável para que replays não dupliquem.
@Slf4j
@Component
@Primary
public class NotificationSentOutboxWriter implements PublishNotificationEventPort {

    private static final String EVENT_TYPE = "NOTIFICATION_SENT";
    private static final int EVENT_VERSION = 1;
    private static final String PRODUCER = "ms-notification";
    private static final String STATUS_PENDING = "PENDING";

    private final NotificationOutboxRepository repository;
    private final NotificationProperties props;
    private final JsonMapper jsonMapper;
    private final MeterRegistry meterRegistry;

    public NotificationSentOutboxWriter(NotificationOutboxRepository repository,
                                        NotificationProperties props,
                                        JsonMapper jsonMapper,
                                        MeterRegistry meterRegistry) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    @Override
    public void publish(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");

        UUID notificationId = notification.getId().value();
        UUID userId = notification.getUserId().value();
        Instant occurredAt = Instant.now();

        String correlationId = MDC.get("correlationId");
        UUID causationId = parseUuid(MDC.get("causationId"));

        var data = new NotificationSentDataV1(
                notificationId, userId, notification.getChannel().name(), null, occurredAt);

        var envelope = new EventEnvelope<>(
                notificationId, EVENT_TYPE, EVENT_VERSION, occurredAt, PRODUCER, correlationId, causationId, data);

        String payload = jsonMapper.writeValueAsString(envelope);

        var doc = NotificationOutboxDocument.builder()
                .id(notificationId)
                .aggregateId(notificationId)
                .eventType(EVENT_TYPE)
                .eventVersion(EVENT_VERSION)
                .topic(props.getTopics().getNotificationSent())
                .partitionKey(userId.toString())
                .payload(payload)
                .correlationId(correlationId)
                .causationId(causationId)
                .status(STATUS_PENDING)
                .attempts(0)
                .occurredAt(occurredAt)
                .createdAt(occurredAt)
                .updatedAt(occurredAt)
                .build();

        try {
            repository.insert(doc);
            meterRegistry.counter("ecofy.notification.outbox.created.total", "event_type", EVENT_TYPE).increment();
            log.debug("[NotificationSentOutboxWriter] - [publish] -> Evento gravado na Outbox (PENDING) notificationId={} correlationId={}",
                    notificationId, correlationId);
        } catch (DuplicateKeyException dup) {
            // Índice único em aggregateId: notification.sent já registrado para esta notificação → idempotente.
            meterRegistry.counter("ecofy.notification.duplicate.ignored.total", "stage", "outbox").increment();
            log.debug("[NotificationSentOutboxWriter] - [publish] -> Evento já registrado (idempotente) notificationId={}", notificationId);
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
