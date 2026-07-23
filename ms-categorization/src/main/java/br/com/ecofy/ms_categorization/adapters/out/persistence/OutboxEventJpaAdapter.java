package br.com.ecofy.ms_categorization.adapters.out.persistence;

import br.com.ecofy.ms_categorization.adapters.out.persistence.entity.OutboxEventEntity;
import br.com.ecofy.ms_categorization.adapters.out.persistence.repository.OutboxEventRepository;
import br.com.ecofy.ms_categorization.core.domain.outbox.OutboxEvent;
import br.com.ecofy.ms_categorization.core.port.out.OutboxEventPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

// Centraliza a persistência e o controle dos eventos da outbox.
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventJpaAdapter implements OutboxEventPortOut {

    private final OutboxEventRepository repository;

    // Registra o evento na mesma transação da operação de domínio.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(OutboxEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        repository.save(toEntity(event));

        log.debug("[OutboxEventJpaAdapter] - [save] -> outbox gravada eventId={} eventType={} aggregateId={}",
                event.getId(), event.getEventType(), event.getAggregateId());
    }

    // Reserva os eventos elegíveis para publicação sem prolongar os bloqueios.
    @Override
    @Transactional
    public List<OutboxEvent> claimPendingBatch(int batchSize, Instant now) {
        List<OutboxEventEntity> locked = repository.lockEligibleBatch(now, batchSize);
        if (locked.isEmpty()) {
            return List.of();
        }

        return locked.stream()
                .map(entity -> {
                    OutboxEvent domain = toDomain(entity);
                    domain.markProcessing(now);
                    applyMutableState(domain, entity);
                    return domain;
                })
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(OutboxEvent event, Instant publishedAt) {
        event.markPublished(publishedAt);
        applyToStored(event);
    }

    @Override
    @Transactional
    public void markFailed(OutboxEvent event, String errorCode, Instant nextAttemptAt, Instant now) {
        event.markFailed(errorCode, nextAttemptAt, now);
        applyToStored(event);
    }

    @Override
    @Transactional
    public void markDiscarded(OutboxEvent event, String errorCode, Instant now) {
        event.markDiscarded(errorCode, now);
        applyToStored(event);
    }

    @Override
    @Transactional
    public int releaseStuck(Duration timeout, Instant now) {
        Instant threshold = now.minus(timeout);
        List<OutboxEventEntity> stuck = repository.findStuckProcessing(threshold);

        for (OutboxEventEntity entity : stuck) {
            OutboxEvent domain = toDomain(entity);
            domain.releaseStuckProcessing(now);
            applyMutableState(domain, entity);
        }

        if (!stuck.isEmpty()) {
            log.warn("[OutboxEventJpaAdapter] - [releaseStuck] -> {} registros presos em PROCESSING liberados", stuck.size());
        }
        return stuck.size();
    }

    @Override
    @Transactional
    public int deletePublishedOlderThan(Duration retention, Instant now, int maxRows) {
        return repository.deletePublishedBatch(now.minus(retention), maxRows);
    }

    @Override
    @Transactional(readOnly = true)
    public Instant oldestPendingCreatedAt() {
        return repository.findOldestUnpublishedCreatedAt();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatusProcessingStuck(Duration timeout, Instant now) {
        return repository.countStuckProcessing(now.minus(timeout));
    }

    @Override
    @Transactional(readOnly = true)
    public long countDiscarded() {
        return repository.countDiscarded();
    }

    private void applyToStored(OutboxEvent event) {
        OutboxEventEntity entity = repository.findById(event.getId())
                .orElseThrow(() -> new IllegalStateException("Outbox event not found for id: " + event.getId()));
        applyMutableState(event, entity);
    }

    // Copia o estado mutável sem alterar a identidade do evento.
    private static void applyMutableState(OutboxEvent domain, OutboxEventEntity entity) {
        entity.setStatus(domain.getStatus());
        entity.setAttempts(domain.getAttempts());
        entity.setNextAttemptAt(domain.getNextAttemptAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setProcessingStartedAt(domain.getProcessingStartedAt());
        entity.setPublishedAt(domain.getPublishedAt());
        entity.setLastErrorCode(domain.getLastErrorCode());
        entity.setLastErrorAt(domain.getLastErrorAt());
    }

    private static OutboxEventEntity toEntity(OutboxEvent d) {
        return OutboxEventEntity.builder()
                .id(d.getId())
                .aggregateType(d.getAggregateType())
                .aggregateId(d.getAggregateId())
                .eventType(d.getEventType())
                .eventVersion(d.getEventVersion())
                .topic(d.getTopic())
                .partitionKey(d.getPartitionKey())
                .payload(d.getPayload())
                .correlationId(d.getCorrelationId())
                .causationId(d.getCausationId())
                .status(d.getStatus())
                .attempts(d.getAttempts())
                .nextAttemptAt(d.getNextAttemptAt())
                .occurredAt(d.getOccurredAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .processingStartedAt(d.getProcessingStartedAt())
                .publishedAt(d.getPublishedAt())
                .lastErrorCode(d.getLastErrorCode())
                .lastErrorAt(d.getLastErrorAt())
                .build();
    }

    private static OutboxEvent toDomain(OutboxEventEntity e) {
        return new OutboxEvent(
                e.getId(),
                e.getAggregateType(),
                e.getAggregateId(),
                e.getEventType(),
                e.getEventVersion(),
                e.getTopic(),
                e.getPartitionKey(),
                e.getPayload(),
                e.getCorrelationId(),
                e.getCausationId(),
                e.getStatus(),
                e.getAttempts(),
                e.getNextAttemptAt(),
                e.getOccurredAt(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getProcessingStartedAt(),
                e.getPublishedAt(),
                e.getLastErrorCode(),
                e.getLastErrorAt()
        );
    }
}
