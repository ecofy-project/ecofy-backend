package br.com.ecofy.ms_users.adapters.in.kafka;

import br.com.ecofy.ms_users.adapters.in.kafka.dto.AuthUserCreatedEventMessage;
import br.com.ecofy.ms_users.core.application.service.AuthUserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserCreatedEventConsumer {

    private final AuthUserSyncService authUserSyncService;

    @KafkaListener(
            id = "authUserCreatedEventConsumer",
            topics = "${ecofy.users.topics.auth-user-created:auth.user.created}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    // Consome o evento AuthUserCreated do Kafka e delega a sincronização do usuário para o AuthUserSyncService, com logs e tratamento de falhas para retry/DLT.
    public void consume(
            AuthUserCreatedEventMessage msg,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(name = KafkaHeaders.OFFSET, required = false) Long offset,
            @Header(name = KafkaHeaders.RECEIVED_TIMESTAMP, required = false) Long timestamp
    ) {
        Objects.requireNonNull(msg, "msg must not be null");

        final long startNs = System.nanoTime();

        log.info(
                "[AuthUserCreatedEventConsumer] consume status=received topic={} partition={} offset={} ts={} userId={} extAuthId={} hasEmail={} hasPhone={} hasFullName={}",
                v(topic),
                v(partition),
                v(offset),
                v(timestamp),
                v(msg.userId()),
                v(msg.externalAuthId()),
                hasText(msg.email()),
                hasText(msg.phone()),
                hasText(msg.fullName())
        );

        try {
            authUserSyncService.onAuthUserCreated(
                    msg.userId(),
                    msg.externalAuthId(),
                    msg.fullName(),
                    msg.email(),
                    msg.phone()
            );

            final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

            log.info(
                    "[AuthUserCreatedEventConsumer] consume status=processed topic={} partition={} offset={} userId={} extAuthId={} elapsedMs={}",
                    v(topic),
                    v(partition),
                    v(offset),
                    v(msg.userId()),
                    v(msg.externalAuthId()),
                    elapsedMs
            );

        } catch (RuntimeException ex) {
            final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

            log.error(
                    "[AuthUserCreatedEventConsumer] consume status=failed topic={} partition={} offset={} userId={} extAuthId={} elapsedMs={}",
                    v(topic),
                    v(partition),
                    v(offset),
                    v(msg.userId()),
                    v(msg.externalAuthId()),
                    elapsedMs,
                    ex
            );

            // rethrow para retry/backoff/DLT funcionar (via error handler do container)
            throw ex;

        } catch (Exception ex) {
            final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

            log.error(
                    "[AuthUserCreatedEventConsumer] consume status=failed topic={} partition={} offset={} userId={} extAuthId={} elapsedMs={}",
                    v(topic),
                    v(partition),
                    v(offset),
                    v(msg.userId()),
                    v(msg.externalAuthId()),
                    elapsedMs,
                    ex
            );

            // Exception checked -> encapsula para permitir rethrow
            throw new RuntimeException(ex);
        }
    }

    // Valida se a string possui conteúdo útil (não nula e não em branco).
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Normaliza valores para log, substituindo null por "-" para facilitar leitura e evitar NPE.
    private static String v(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

}
