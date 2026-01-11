package br.com.ecofy.ms_notification.adapters.out.messaging;

import br.com.ecofy.ms_notification.adapters.out.messaging.dto.NotificationSentEvent;
import br.com.ecofy.ms_notification.config.NotificationProperties;
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

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String notificationSentTopic;

    public NotificationEventsKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate, NotificationProperties props) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        Objects.requireNonNull(props, "props must not be null");
        Objects.requireNonNull(props.getTopics(), "props.topics must not be null");

        this.notificationSentTopic = Objects.requireNonNull(
                props.getTopics().getNotificationSent(),
                "props.topics.notificationSent must not be null"
        );
    }

    @Override
    public void publish(NotificationSentEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        String key = event.notificationId() != null
                ? event.notificationId().toString()
                : UUID.randomUUID().toString();

        kafkaTemplate.send(notificationSentTopic, key, event)
                .whenComplete((SendResult<String, Object> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error(
                                "[NotificationEventsKafkaAdapter] - [publish] -> failed to publish notification.sent topic={} key={} notificationId={}",
                                notificationSentTopic,
                                key,
                                event.notificationId(),
                                ex
                        );
                        return;
                    }

                    var meta = result.getRecordMetadata();
                    log.debug(
                            "[NotificationEventsKafkaAdapter] - [publish] -> published notification.sent topic={} key={} notificationId={} partition={} offset={}",
                            notificationSentTopic,
                            key,
                            event.notificationId(),
                            meta.partition(),
                            meta.offset()
                    );
                });
    }
}
