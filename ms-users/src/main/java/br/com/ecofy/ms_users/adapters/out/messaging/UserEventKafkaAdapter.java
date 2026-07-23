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

    // Inicializa o adapter de publicação em Kafka, validando dependências e resolvendo o tópico ecoUserEvent via UsersProperties.
    public UserEventKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate, UsersProperties props) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        Objects.requireNonNull(props, "props must not be null");
        Objects.requireNonNull(props.topics(), "props.topics must not be null");

        this.ecoUserEventTopic = Objects.requireNonNull(
                props.topics().ecoUserEvent(),
                TOPIC_PROP_NAME + " must not be null"
        );
    }

    // Publica o evento de criação de perfil de usuário no tópico ecoUserEvent.
    @Override
    public void publishUserProfileCreated(String key, Object payload) {
        publish("publishUserProfileCreated", key, payload);
    }

    // Publica o evento de atualização de perfil de usuário no tópico ecoUserEvent.
    @Override
    public void publishUserProfileUpdated(String key, Object payload) {
        publish("publishUserProfileUpdated", key, payload);
    }

    // Centraliza a publicação em Kafka (gera key quando ausente, envia via KafkaTemplate e registra sucesso/falha).
    private void publish(String operation, String key, Object payload) {
        Objects.requireNonNull(payload, "payload must not be null");

        String safeKey = (key == null || key.isBlank()) ? UUID.randomUUID().toString() : key.trim();

        log.debug(
                "[UserEventKafkaAdapter] - [{}] -> Publicando evento topic={} keyPresent={} payloadType={}",
                operation,
                ecoUserEventTopic,
                key != null && !key.isBlank(),
                payload.getClass().getSimpleName()
        );

        kafkaTemplate.send(ecoUserEventTopic, safeKey, payload)
                .whenComplete((SendResult<String, Object> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error(
                                "[UserEventKafkaAdapter] - [{}] -> Falha ao publicar evento topic={} key={} payloadType={}",
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
