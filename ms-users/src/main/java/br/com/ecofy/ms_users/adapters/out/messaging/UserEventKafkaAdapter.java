package br.com.ecofy.ms_users.adapters.out.messaging;

import br.com.ecofy.ms_users.config.UsersProperties;
import br.com.ecofy.ms_users.core.port.out.PublishUserEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class UserEventKafkaAdapter implements PublishUserEventPort {

    private static final String TOPIC_PROP_NAME = "usersProperties.topics.ecoUserEvent";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ecoUserEventTopic;

    public UserEventKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate, UsersProperties props) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        Objects.requireNonNull(props, "props must not be null");
        Objects.requireNonNull(props.topics(), "props.topics must not be null");

        this.ecoUserEventTopic = Objects.requireNonNull(
                props.topics().ecoUserEvent(),
                TOPIC_PROP_NAME + " must not be null"
        );
    }

    @Override
    public void publishUserProfileCreated(String key, Object payload) {
        publish("publishUserProfileCreated", key, payload);
    }

    @Override
    public void publishUserProfileUpdated(String key, Object payload) {
        publish("publishUserProfileUpdated", key, payload);
    }

    private void publish(String operation, String key, Object payload) {
        Objects.requireNonNull(payload, "payload must not be null");

        String safeKey = (key == null || key.isBlank()) ? UUID.randomUUID().toString() : key.trim();

        log.debug(
                "[UserEventKafkaAdapter] - [{}] -> publishing topic={} keyPresent={} payloadType={}",
                operation,
                ecoUserEventTopic,
                key != null && !key.isBlank(),
                payload.getClass().getSimpleName()
        );

        kafkaTemplate.send(ecoUserEventTopic, safeKey, payload)
                .whenComplete((SendResult<String, Object> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error(
                                "[UserEventKafkaAdapter] - [{}] -> failed topic={} key={} payloadType={}",
                                operation,
                                ecoUserEventTopic,
                                safeKey,
                                payload.getClass().getSimpleName(),
                                ex
                        );
                        return;
                    }

                    var meta = result.getRecordMetadata();
                    log.info(
                            "[UserEventKafkaAdapter] - [{}] -> published topic={} key={} partition={} offset={} payloadType={}",
                            operation,
                            ecoUserEventTopic,
                            safeKey,
                            meta.partition(),
                            meta.offset(),
                            payload.getClass().getSimpleName()
                    );
                });
    }
}
