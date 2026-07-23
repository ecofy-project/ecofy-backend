package br.com.ecofy.ms_notification.adapters.out.messaging;

import br.com.ecofy.ms_notification.adapters.out.persistence.document.NotificationOutboxDocument;
import br.com.ecofy.ms_notification.adapters.out.persistence.repository.NotificationOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

// Publica os eventos da Outbox no Kafka com reserva atômica, confirmação síncrona, backoff e descarte auditável.
@Slf4j
@Component
public class NotificationOutboxPublisher {

    private static final String PENDING = "PENDING";
    private static final String PROCESSING = "PROCESSING";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String FAILED = "FAILED";
    private static final String DISCARDED = "DISCARDED";

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);
    private static final Duration PUBLISH_TIMEOUT = Duration.ofSeconds(10);

    private final NotificationOutboxRepository repository;
    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public NotificationOutboxPublisher(NotificationOutboxRepository repository,
                                       MongoTemplate mongoTemplate,
                                       @Qualifier("notificationOutboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
                                       MeterRegistry meterRegistry) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate must not be null");
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    @Scheduled(fixedDelayString = "${ecofy.notification.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        Instant now = Instant.now();
        var pageable = PageRequest.of(0, BATCH_SIZE);

        List<NotificationOutboxDocument> eligible = repository.findByStatusOrderByCreatedAtAsc(PENDING, pageable);
        eligible.addAll(repository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(FAILED, now, pageable));

        for (NotificationOutboxDocument doc : eligible) {
            if (!claim(doc.getId())) {
                continue; // outra instância pegou este registro.
            }
            publishOne(doc);
        }
    }

    // Reserva o registro atomicamente, evitando disputa entre instâncias do publisher.
    private boolean claim(java.util.UUID id) {
        Query q = new Query(Criteria.where("_id").is(id).and("status").in(PENDING, FAILED));
        Update u = new Update().set("status", PROCESSING).set("updatedAt", Instant.now()).inc("attempts", 1);
        var result = mongoTemplate.updateFirst(q, u, NotificationOutboxDocument.class);
        return result.getModifiedCount() > 0;
    }

    private void publishOne(NotificationOutboxDocument doc) {
        try {
            var record = new ProducerRecord<>(doc.getTopic(), doc.getPartitionKey(), doc.getPayload());
            addHeaders(record, doc);
            kafkaTemplate.send(record).get(PUBLISH_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            mongoTemplate.updateFirst(
                    new Query(Criteria.where("_id").is(doc.getId())),
                    new Update().set("status", PUBLISHED).set("publishedAt", Instant.now()).set("updatedAt", Instant.now()),
                    NotificationOutboxDocument.class);

            meterRegistry.counter("ecofy.notification.outbox.published.total", "event_type", doc.getEventType()).increment();
            log.info("[NotificationOutboxPublisher] - [publishOne] -> Evento da Outbox publicado com sucesso eventId={} topic={} correlationId={}",
                    doc.getId(), doc.getTopic(), doc.getCorrelationId());

        } catch (Exception e) {
            handleFailure(doc, e);
        }
    }

    private void handleFailure(NotificationOutboxDocument doc, Exception cause) {
        String code = cause.getClass().getSimpleName();
        int attempts = doc.getAttempts();
        if (attempts >= MAX_ATTEMPTS) {
            mongoTemplate.updateFirst(
                    new Query(Criteria.where("_id").is(doc.getId())),
                    new Update().set("status", DISCARDED).set("lastErrorCode", code).set("updatedAt", Instant.now()),
                    NotificationOutboxDocument.class);
            meterRegistry.counter("ecofy.notification.outbox.discarded.total", "event_type", doc.getEventType()).increment();
            log.error("[NotificationOutboxPublisher] - [handleFailure] -> Evento da Outbox descartado após atingir limite de tentativas eventId={} attempts={} error={}",
                    doc.getId(), attempts, code);
            return;
        }
        Instant nextAttemptAt = Instant.now().plus(backoff(attempts));
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(doc.getId())),
                new Update().set("status", FAILED).set("nextAttemptAt", nextAttemptAt)
                        .set("lastErrorCode", code).set("updatedAt", Instant.now()),
                NotificationOutboxDocument.class);
        meterRegistry.counter("ecofy.notification.outbox.failed.total", "event_type", doc.getEventType()).increment();
        log.warn("[NotificationOutboxPublisher] - [handleFailure] -> Falha na publicação; nova tentativa agendada eventId={} attempt={} nextAttemptAt={} error={}",
                doc.getId(), attempts, nextAttemptAt, code);
    }

    private static Duration backoff(int attempts) {
        long millis = (long) (INITIAL_BACKOFF.toMillis() * Math.pow(2, Math.max(0, attempts - 1)));
        return Duration.ofMillis(Math.min(millis, MAX_BACKOFF.toMillis()));
    }

    private static void addHeaders(ProducerRecord<String, String> record, NotificationOutboxDocument doc) {
        if (doc.getCorrelationId() != null) {
            record.headers().add(header("X-Correlation-Id", doc.getCorrelationId()));
        }
        record.headers().add(header("eventId", doc.getId().toString()));
        record.headers().add(header("eventType", doc.getEventType()));
        record.headers().add(header("eventVersion", String.valueOf(doc.getEventVersion())));
        if (doc.getCausationId() != null) {
            record.headers().add(header("causationId", doc.getCausationId().toString()));
        }
    }

    private static RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
