package br.com.ecofy.ms_notification.adapters.out.messaging;

import br.com.ecofy.ms_notification.adapters.out.messaging.dto.NotificationSentEvent;
import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.domain.Notification;
import br.com.ecofy.ms_notification.core.port.out.PublishNotificationEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class NotificationEventsKafkaAdapter implements PublishNotificationEventPort {

    private static final String EVENT_TYPE_NOTIFICATION_SENT = "notification.sent";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String notificationSentTopic;

    public NotificationEventsKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate, NotificationProperties props) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        Objects.requireNonNull(props, "props must not be null");
        Objects.requireNonNull(props.getTopics(), "props.topics must not be null");

        this.notificationSentTopic = requireNonBlank(
                props.getTopics().getNotificationSent(),
                "props.topics.notificationSent"
        );
    }

    @Override
    public void publish(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");

        // Adapter converte o domínio -> DTO externo (mantém o core livre do DTO Kafka).
        final NotificationSentEvent event = NotificationSentEvent.from(notification);

        final String topic = notificationSentTopic;
        final String key = resolveKey(event);

        // Publica o objeto (serialização configurada via ProducerFactory / JsonSerializer etc.)
        kafkaTemplate.send(topic, key, event)
                .whenComplete((SendResult<String, Object> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error(
                                "[NotificationEventsKafkaAdapter] - [publish] -> status=failed eventType={} topic={} key={} notificationId={} userId={}",
                                EVENT_TYPE_NOTIFICATION_SENT,
                                topic,
                                key,
                                safe(event.notificationId()),
                                safe(event.userId()),
                                ex
                        );
                        return;
                    }

                    var meta = result.getRecordMetadata();
                    log.info(
                            "[NotificationEventsKafkaAdapter] - [publish] -> status=published eventType={} topic={} key={} notificationId={} userId={} partition={} offset={}",
                            EVENT_TYPE_NOTIFICATION_SENT,
                            topic,
                            key,
                            safe(event.notificationId()),
                            safe(event.userId()),
                            meta.partition(),
                            meta.offset()
                    );
                });
    }

    // Resolve a chave do Kafka: preferencialmente notificationId; fallback UUID para evitar chave vazia.
    private static String resolveKey(NotificationSentEvent event) {
        if (event.notificationId() != null) {
            return event.notificationId().toString();
        }
        return UUID.randomUUID().toString();
    }

    // Valida string obrigatória e normaliza (trim).
    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v.trim();
    }

    // Normaliza valores em logs (evita null literal e facilita observabilidade).
    private static String safe(Object v) {
        return v == null ? "-" : String.valueOf(v);
    }

}
