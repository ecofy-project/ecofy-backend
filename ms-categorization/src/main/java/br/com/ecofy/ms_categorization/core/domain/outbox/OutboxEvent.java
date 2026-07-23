package br.com.ecofy.ms_categorization.core.domain.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// Representa um evento da outbox e controla suas transições de estado.
public class OutboxEvent {

    private static final Map<OutboxStatus, Set<OutboxStatus>> ALLOWED_TRANSITIONS = Map.of(
            OutboxStatus.PENDING, Set.of(OutboxStatus.PROCESSING),
            OutboxStatus.PROCESSING, Set.of(OutboxStatus.PUBLISHED, OutboxStatus.FAILED, OutboxStatus.PENDING),
            OutboxStatus.FAILED, Set.of(OutboxStatus.PROCESSING, OutboxStatus.DISCARDED),
            OutboxStatus.PUBLISHED, Set.of(),
            OutboxStatus.DISCARDED, Set.of()
    );

    private final UUID id;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String eventType;
    private final int eventVersion;
    private final String topic;
    private final String partitionKey;
    private final String payload;
    private final String correlationId;
    private final UUID causationId;
    private final Instant occurredAt;
    private final Instant createdAt;

    private OutboxStatus status;
    private int attempts;
    private Instant nextAttemptAt;
    private Instant updatedAt;
    private Instant processingStartedAt;
    private Instant publishedAt;
    private String lastErrorCode;
    private Instant lastErrorAt;

    public OutboxEvent(UUID id,
                       String aggregateType,
                       UUID aggregateId,
                       String eventType,
                       int eventVersion,
                       String topic,
                       String partitionKey,
                       String payload,
                       String correlationId,
                       UUID causationId,
                       OutboxStatus status,
                       int attempts,
                       Instant nextAttemptAt,
                       Instant occurredAt,
                       Instant createdAt,
                       Instant updatedAt,
                       Instant processingStartedAt,
                       Instant publishedAt,
                       String lastErrorCode,
                       Instant lastErrorAt) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.eventVersion = eventVersion;
        this.topic = Objects.requireNonNull(topic, "topic must not be null");
        this.partitionKey = Objects.requireNonNull(partitionKey, "partitionKey must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNullElse(updatedAt, createdAt);
        this.processingStartedAt = processingStartedAt;
        this.publishedAt = publishedAt;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorAt = lastErrorAt;
    }

    // Cria um evento pendente para persistência transacional.
    public static OutboxEvent createPending(UUID eventId,
                                            String aggregateType,
                                            UUID aggregateId,
                                            String eventType,
                                            int eventVersion,
                                            String topic,
                                            String partitionKey,
                                            String payload,
                                            String correlationId,
                                            UUID causationId,
                                            Instant occurredAt) {
        Instant now = Instant.now();
        return new OutboxEvent(
                eventId, aggregateType, aggregateId, eventType, eventVersion,
                topic, partitionKey, payload, correlationId, causationId,
                OutboxStatus.PENDING, 0, null,
                occurredAt, now, now,
                null, null, null, null
        );
    }

    // Reserva o evento para publicação e registra uma tentativa.
    public void markProcessing(Instant now) {
        transitionTo(OutboxStatus.PROCESSING);
        this.attempts++;
        this.processingStartedAt = now;
        this.updatedAt = now;
    }

    // Confirma a publicação após o aceite do broker.
    public void markPublished(Instant now) {
        transitionTo(OutboxStatus.PUBLISHED);
        this.publishedAt = now;
        this.processingStartedAt = null;
        this.nextAttemptAt = null;
        this.updatedAt = now;
    }

    // Registra a falha e agenda uma nova tentativa.
    public void markFailed(String errorCode, Instant nextAttemptAt, Instant now) {
        transitionTo(OutboxStatus.FAILED);
        this.lastErrorCode = errorCode;
        this.lastErrorAt = now;
        this.nextAttemptAt = nextAttemptAt;
        this.processingStartedAt = null;
        this.updatedAt = now;
    }

    // Descarta o evento após o esgotamento das tentativas.
    public void markDiscarded(String errorCode, Instant now) {
        transitionTo(OutboxStatus.DISCARDED);
        this.lastErrorCode = errorCode;
        this.lastErrorAt = now;
        this.nextAttemptAt = null;
        this.processingStartedAt = null;
        this.updatedAt = now;
    }

    // Devolve o evento interrompido à fila de publicação.
    public void releaseStuckProcessing(Instant now) {
        transitionTo(OutboxStatus.PENDING);
        this.processingStartedAt = null;
        this.updatedAt = now;
    }

    public boolean hasExhaustedAttempts(int maxAttempts) {
        return attempts >= maxAttempts;
    }

    // Valida e aplica uma transição permitida de estado.
    private void transitionTo(OutboxStatus target) {
        Set<OutboxStatus> allowed = ALLOWED_TRANSITIONS.get(status);
        if (allowed == null || !allowed.contains(target)) {
            throw new IllegalStateException("Outbox event " + id + " cannot transition from " + status
                    + " to " + target);
        }
        this.status = target;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public String getTopic() {
        return topic;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public String getPayload() {
        return payload;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public UUID getCausationId() {
        return causationId;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public Instant getLastErrorAt() {
        return lastErrorAt;
    }
}
