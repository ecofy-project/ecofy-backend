package br.com.ecofy.auth.adapters.out.messaging;

import br.com.ecofy.auth.core.domain.event.PasswordResetRequestedEvent;
import br.com.ecofy.auth.core.domain.event.UserAuthenticatedEvent;
import br.com.ecofy.auth.core.domain.event.UserEmailConfirmedEvent;
import br.com.ecofy.auth.core.domain.event.UserRegisteredEvent;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class AuthEventsKafkaAdapter implements PublishAuthEventPort {

    private static final String TOPIC_USER_REGISTERED          = "auth.user.registered";
    private static final String TOPIC_USER_EMAIL_CONFIRMED     = "auth.user.email-confirmed";
    private static final String TOPIC_USER_AUTHENTICATED       = "auth.user.authenticated";
    private static final String TOPIC_PASSWORD_RESET_REQUESTED = "auth.user.password-reset-requested";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Injeta o KafkaTemplate e garante que ele não seja nulo para publicação de eventos.
    public AuthEventsKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
    }

    // Publica o evento de usuário registrado no tópico correspondente usando o userId como key.
    @Override
    public void publish(UserRegisteredEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String key = event.user().id().value().toString();

        log.debug("[AuthEventsKafkaAdapter] - [publish(UserRegisteredEvent)] -> Enviando evento topic={} key={}",
                TOPIC_USER_REGISTERED, key);

        sendEvent(TOPIC_USER_REGISTERED, key, event);
    }

    // Publica o evento de e-mail confirmado no tópico correspondente usando o userId como key.
    @Override
    public void publish(UserEmailConfirmedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String key = event.user().id().value().toString();

        log.debug("[AuthEventsKafkaAdapter] - [publish(UserEmailConfirmedEvent)] -> Enviando evento topic={} key={}",
                TOPIC_USER_EMAIL_CONFIRMED, key);

        sendEvent(TOPIC_USER_EMAIL_CONFIRMED, key, event);
    }

    // Publica o evento de autenticação realizada no tópico correspondente usando o userId como key.
    @Override
    public void publish(UserAuthenticatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String key = event.user().id().value().toString();

        log.debug("[AuthEventsKafkaAdapter] - [publish(UserAuthenticatedEvent)] -> Enviando evento topic={} key={}",
                TOPIC_USER_AUTHENTICATED, key);

        sendEvent(TOPIC_USER_AUTHENTICATED, key, event);
    }

    // Publica o evento de solicitação de reset de senha no tópico correspondente usando o userId como key.
    @Override
    public void publish(PasswordResetRequestedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String key = event.user().id().value().toString();

        log.debug("[AuthEventsKafkaAdapter] - [publish(PasswordResetRequestedEvent)] -> Enviando evento topic={} key={}",
                TOPIC_PASSWORD_RESET_REQUESTED, key);

        sendEvent(TOPIC_PASSWORD_RESET_REQUESTED, key, event);
    }

    // Envia o payload para o Kafka de forma assíncrona e registra sucesso (partition/offset) ou erro no log.
    private void sendEvent(String topic, String key, Object payload) {

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "[AuthEventsKafkaAdapter] - [sendEvent] -> ERRO ao enviar evento topic={} key={} error={}",
                        topic, key, ex.getMessage(), ex
                );
                return;
            }

            RecordMetadata md = result.getRecordMetadata();
            log.debug(
                    "[AuthEventsKafkaAdapter] - [sendEvent] -> Evento enviado topic={} key={} partition={} offset={}",
                    topic, key, md.partition(), md.offset()
            );
        });
    }

}
