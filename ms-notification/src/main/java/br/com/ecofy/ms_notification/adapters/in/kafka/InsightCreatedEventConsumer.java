package br.com.ecofy.ms_notification.adapters.in.kafka;

import br.com.ecofy.ms_notification.adapters.in.kafka.dto.InsightCreatedEventMessage;
import br.com.ecofy.ms_notification.adapters.in.kafka.mapper.InboundEventMapper;
import br.com.ecofy.ms_notification.core.application.command.HandleDomainEventCommand;
import br.com.ecofy.ms_notification.core.port.in.HandleDomainEventNotificationUseCase;
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
public class InsightCreatedEventConsumer {

    private final InboundEventMapper mapper;
    private final HandleDomainEventNotificationUseCase useCase;

    @KafkaListener(
            id = "insightCreatedEventConsumer",
            topics = "${notification.topics.insight-created:eco.insight.created}",
            containerFactory = "insightCreatedKafkaListenerContainerFactory"
    )
    public void consume(
            InsightCreatedEventMessage message,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(name = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        Objects.requireNonNull(message, "message must not be null");

        final long startNs = System.nanoTime();

        final String eventId = message.metadata() != null ? message.metadata().eventId() : null;
        final String userId = String.valueOf(message.userId());

        log.info(
                "[InsightCreatedEventConsumer] - [consume] -> status=received topic={} partition={} offset={} userId={} eventId={}",
                safe(topic), safe(partition), safe(offset), safe(userId), safe(eventId)
        );

        // Se você quiser detalhes mínimos de payload sem poluir INFO:
         if (log.isDebugEnabled()) {
             log.debug("[InsightCreatedEventConsumer] - [consume] -> hasMetadata={}", message.metadata() != null);
         }

        try {
            final HandleDomainEventCommand command = mapper.fromInsightCreated(message);
            final String idempotencyKey = command != null ? command.idempotencyKey() : null;

            useCase.handle(command);

            final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

            log.info(
                    "[InsightCreatedEventConsumer] - [consume] -> status=processed topic={} partition={} offset={} userId={} eventId={} idempotencyKey={} elapsedMs={}",
                    safe(topic), safe(partition), safe(offset), safe(userId), safe(eventId), safe(idempotencyKey), elapsedMs
            );
        } catch (Exception ex) {
            final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

            log.error(
                    "[InsightCreatedEventConsumer] - [consume] -> status=failed topic={} partition={} offset={} userId={} eventId={} elapsedMs={}",
                    safe(topic), safe(partition), safe(offset), safe(userId), safe(eventId), elapsedMs, ex
            );

            throw ex;
        }
    }

    /**
     * Normaliza valores para logs: evita "null" literal e facilita leitura em observabilidade.
     */
    private static String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
