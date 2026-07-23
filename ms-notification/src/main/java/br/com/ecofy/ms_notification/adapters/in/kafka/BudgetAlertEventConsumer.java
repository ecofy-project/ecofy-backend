package br.com.ecofy.ms_notification.adapters.in.kafka;

import br.com.ecofy.ms_notification.adapters.in.kafka.dto.BudgetAlertEventMessage;
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
public class BudgetAlertEventConsumer {

    private final InboundEventMapper mapper;
    private final HandleDomainEventNotificationUseCase useCase;

    // Consome eventos eco.budget.alert do Kafka, mapeia para comando de aplicação e delega o processamento ao caso de uso de notificação com logs e fail-fast.
    @KafkaListener(
            id = "budgetAlertEventConsumer",
            // Evita dependência de SpEL/bean (resiliente e padrão Spring)
            topics = "${ecofy.notification.topics.budget-alert:eco.budget.alert}",
            containerFactory = "budgetAlertKafkaListenerContainerFactory"
    )
    public void consume(
            BudgetAlertEventMessage message,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(name = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        Objects.requireNonNull(message, "message must not be null");

        final long startNs = System.nanoTime();

        final String eventId = message.eventId() != null ? message.eventId().toString() : null;
        final String userId = message.data() != null ? String.valueOf(message.data().userId()) : "null";

        log.info(
                "[BudgetAlertEventConsumer] - [consume] -> Evento Kafka recebido topic={} partition={} offset={} userId={} eventId={}",
                safe(topic), safe(partition), safe(offset), safe(userId), safe(eventId)
        );

        try {
            final HandleDomainEventCommand command = mapper.fromBudgetAlert(message);
            final String idemKey = command != null ? command.idempotencyKey() : null;

            useCase.handle(command);

            final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

            log.info(
                    "[BudgetAlertEventConsumer] - [consume] -> Evento Kafka processado com sucesso topic={} partition={} offset={} userId={} eventId={} idemKey={} elapsedMs={}",
                    safe(topic), safe(partition), safe(offset), safe(userId), safe(eventId), safe(idemKey), elapsedMs
            );
        } catch (Exception ex) {
            final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

            log.error(
                    "[BudgetAlertEventConsumer] - [consume] -> Falha ao processar evento Kafka topic={} partition={} offset={} userId={} eventId={} elapsedMs={}",
                    safe(topic), safe(partition), safe(offset), safe(userId), safe(eventId), elapsedMs, ex
            );

            throw ex;
        }
    }

    // Normaliza valores nulos para string segura em logs, evitando NPE e mantendo logs consistentes.
    private static String safe(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

}
