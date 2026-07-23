package br.com.ecofy.ms_categorization.adapters.out.outbox;

import br.com.ecofy.ms_categorization.adapters.correlation.CorrelationContext;
import br.com.ecofy.ms_categorization.adapters.out.messaging.EventTypes;
import br.com.ecofy.ms_categorization.adapters.out.messaging.dto.CategorizationAppliedDataV1;
import br.com.ecofy.ms_categorization.adapters.out.messaging.dto.EventEnvelope;
import br.com.ecofy.ms_categorization.adapters.out.messaging.dto.TransactionCategorizedDataV1;
import br.com.ecofy.ms_categorization.config.CategorizationProperties;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizationAppliedDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizedTransactionDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.outbox.OutboxEvent;
import br.com.ecofy.ms_categorization.core.port.out.OutboxEventPortOut;
import br.com.ecofy.ms_categorization.core.port.out.PublishCategorizedTransactionEventPortOut;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

// Persiste o evento na Outbox dentro da transação do caso de uso, eliminando o dual write com o Kafka.
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class OutboxWriterAdapter implements PublishCategorizedTransactionEventPortOut {

    private final OutboxEventPortOut outboxPort;
    private final ObjectMapper objectMapper;
    private final CategorizationProperties props;
    private final MeterRegistry meterRegistry;

    @Override
    public void publish(CategorizedTransactionDomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        TransactionCategorizedDataV1 data = new TransactionCategorizedDataV1(
                event.transactionId(),
                event.importJobId(),
                event.externalId(),
                event.transactionDate(),
                event.amount(),
                event.currency() != null ? event.currency().getCurrencyCode() : null,
                event.categoryId(),
                event.mode()
        );

        enqueue(
                event.eventId(),
                EventTypes.TRANSACTION_CATEGORIZED,
                EventTypes.TRANSACTION_CATEGORIZED_VERSION,
                props.getTopics().getTransactionCategorized(),
                event.transactionId(),
                data,
                event.occurredAt()
        );
    }

    @Override
    public void publish(CategorizationAppliedDomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        CategorizationAppliedDataV1 data = new CategorizationAppliedDataV1(
                event.transactionId(),
                event.categoryId(),
                event.ruleId(),
                event.mode(),
                event.score(),
                event.suggestionId()
        );

        enqueue(
                event.eventId(),
                EventTypes.CATEGORIZATION_APPLIED,
                EventTypes.CATEGORIZATION_APPLIED_VERSION,
                props.getTopics().getCategorizationApplied(),
                event.transactionId(),
                data,
                event.occurredAt()
        );
    }

    private void enqueue(UUID eventId,
                         String eventType,
                         int eventVersion,
                         String topic,
                         UUID transactionId,
                         Object data,
                         java.time.Instant occurredAt) {

        String correlationId = CorrelationContext.currentCorrelationIdOrGenerate();
        UUID causationId = CorrelationContext.currentCausationId();

        EventEnvelope<Object> envelope = new EventEnvelope<>(
                eventId,
                eventType,
                eventVersion,
                occurredAt,
                EventTypes.PRODUCER,
                correlationId,
                causationId,
                data
        );

        // Chave de partição = transactionId (agregado deste fluxo): estável entre
        // republicações, ordena por transação e distribui a carga. Ver ADR-004 — userId
        // seria preferível para ordenação por usuário, mas o fluxo de categorização ainda
        // não carrega esse dado.
        String partitionKey = transactionId.toString();

        OutboxEvent outboxEvent = OutboxEvent.createPending(
                eventId,
                EventTypes.AGGREGATE_TYPE_TRANSACTION,
                transactionId,
                eventType,
                eventVersion,
                topic,
                partitionKey,
                serialize(envelope, eventType),
                correlationId,
                causationId,
                occurredAt
        );

        // Participa da transação corrente do caso de uso (MANDATORY no adapter):
        // rollback da categorização = rollback da outbox, e vice-versa.
        outboxPort.save(outboxEvent);

        meterRegistry.counter("ecofy.categorization.outbox.created.total",
                "event_type", eventType).increment();

        log.info("[OutboxWriterAdapter] - [enqueue] -> Evento gravado na Outbox (PENDING) eventId={} eventType={} topic={} correlationId={}",
                eventId, eventType, topic, correlationId);
    }

    private String serialize(EventEnvelope<Object> envelope, String eventType) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            // Falha de serialização deve ABORTAR a transação: commitar a categorização
            // sem o evento correspondente é exatamente o que a outbox proíbe.
            throw new IllegalStateException("Failed to serialize outbox envelope for " + eventType, e);
        }
    }
}
