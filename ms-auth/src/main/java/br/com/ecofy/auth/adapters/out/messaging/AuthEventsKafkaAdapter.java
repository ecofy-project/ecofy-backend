package br.com.ecofy.auth.adapters.out.messaging;

import br.com.ecofy.auth.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.auth.adapters.out.messaging.dto.AuthUserRegisteredMessage;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.event.PasswordResetRequestedEvent;
import br.com.ecofy.auth.core.domain.event.UserAuthenticatedEvent;
import br.com.ecofy.auth.core.domain.event.UserEmailConfirmedEvent;
import br.com.ecofy.auth.core.domain.event.UserRegisteredEvent;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

// Publica eventos de autenticação nos tópicos correspondentes do Kafka.
@Component
@Slf4j
public class AuthEventsKafkaAdapter implements PublishAuthEventPort {

    private static final String DEFAULT_TOPIC_USER_REGISTERED = "auth.user.registered";
    private static final String TOPIC_USER_EMAIL_CONFIRMED = "auth.user.email-confirmed";
    private static final String TOPIC_USER_AUTHENTICATED = "auth.user.authenticated";
    private static final String TOPIC_PASSWORD_RESET_REQUESTED = "auth.user.password-reset-requested";

    private static final String EVENT_TYPE_USER_REGISTERED = "AUTH_USER_REGISTERED";
    private static final int EVENT_VERSION_USER_REGISTERED = 1;
    private static final String PRODUCER = "ms-auth";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String userRegisteredTopic;

    public AuthEventsKafkaAdapter(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${ecofy.auth.kafka.user-registered-topic:auth.user.registered}")
            String userRegisteredTopic
    ) {
        this.kafkaTemplate = Objects.requireNonNull(
                kafkaTemplate,
                "kafkaTemplate must not be null"
        );

        this.userRegisteredTopic =
                (userRegisteredTopic == null || userRegisteredTopic.isBlank())
                        ? DEFAULT_TOPIC_USER_REGISTERED
                        : userRegisteredTopic;
    }

    // Publica o registro do usuário com metadados de identificação e rastreamento.
    @Override
    public void publish(UserRegisteredEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        AuthUser user = event.user();
        String authUserId = user.id().value().toString();
        String eventId = UUID.randomUUID().toString();
        String correlationId = resolveCorrelationId();

        AuthUserRegisteredMessage message = new AuthUserRegisteredMessage(
                null,
                authUserId,
                user.fullName(),
                user.email().value(),
                null,
                new AuthUserRegisteredMessage.EventMetadata(
                        eventId,
                        event.occurredAt(),
                        correlationId,
                        PRODUCER
                )
        );

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(
                        userRegisteredTopic,
                        authUserId,
                        message
                );

        record.headers()
                .add(CorrelationId.HEADER, bytes(correlationId))
                .add("eventId", bytes(eventId))
                .add("eventType", bytes(EVENT_TYPE_USER_REGISTERED))
                .add(
                        "eventVersion",
                        bytes(String.valueOf(EVENT_VERSION_USER_REGISTERED))
                );

        log.debug(
                "[AuthEventsKafkaAdapter] - [publish(UserRegisteredEvent)] -> Enviando evento topic={} key={} eventId={}",
                userRegisteredTopic,
                authUserId,
                eventId
        );

        sendRecord(record);
    }

    // Resolve o correlation ID atual ou gera um novo identificador.
    private String resolveCorrelationId() {
        String current = CorrelationId.current();

        return (current != null && !current.isBlank())
                ? current
                : CorrelationId.generate();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    // Publica a confirmação de e-mail utilizando o usuário como chave.
    @Override
    public void publish(UserEmailConfirmedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        String key = event.user().id().value().toString();

        log.debug(
                "[AuthEventsKafkaAdapter] - [publish(UserEmailConfirmedEvent)] -> Enviando evento topic={} key={}",
                TOPIC_USER_EMAIL_CONFIRMED,
                key
        );

        sendEvent(TOPIC_USER_EMAIL_CONFIRMED, key, event);
    }

    // Publica a autenticação realizada utilizando o usuário como chave.
    @Override
    public void publish(UserAuthenticatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        String key = event.user().id().value().toString();

        log.debug(
                "[AuthEventsKafkaAdapter] - [publish(UserAuthenticatedEvent)] -> Enviando evento topic={} key={}",
                TOPIC_USER_AUTHENTICATED,
                key
        );

        sendEvent(TOPIC_USER_AUTHENTICATED, key, event);
    }

    // Publica a solicitação de redefinição de senha utilizando o usuário como chave.
    @Override
    public void publish(PasswordResetRequestedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        String key = event.user().id().value().toString();

        log.debug(
                "[AuthEventsKafkaAdapter] - [publish(PasswordResetRequestedEvent)] -> Enviando evento topic={} key={}",
                TOPIC_PASSWORD_RESET_REQUESTED,
                key
        );

        sendEvent(
                TOPIC_PASSWORD_RESET_REQUESTED,
                key,
                event
        );
    }

    // Registra o resultado do envio assíncrono de um evento com headers.
    private void sendRecord(ProducerRecord<String, Object> record) {
        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "[AuthEventsKafkaAdapter] - [sendRecord] -> ERRO ao enviar evento topic={} key={} error={}",
                        record.topic(),
                        record.key(),
                        ex.getMessage(),
                        ex
                );

                return;
            }

            RecordMetadata md = result.getRecordMetadata();

            log.debug(
                    "[AuthEventsKafkaAdapter] - [sendRecord] -> Evento enviado topic={} key={} partition={} offset={}",
                    record.topic(),
                    record.key(),
                    md.partition(),
                    md.offset()
            );
        });
    }

    // Registra o resultado do envio assíncrono de um payload.
    private void sendEvent(
            String topic,
            String key,
            Object payload
    ) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "[AuthEventsKafkaAdapter] - [sendEvent] -> ERRO ao enviar evento topic={} key={} error={}",
                        topic,
                        key,
                        ex.getMessage(),
                        ex
                );

                return;
            }

            RecordMetadata md = result.getRecordMetadata();

            log.debug(
                    "[AuthEventsKafkaAdapter] - [sendEvent] -> Evento enviado topic={} key={} partition={} offset={}",
                    topic,
                    key,
                    md.partition(),
                    md.offset()
            );
        });
    }
}
