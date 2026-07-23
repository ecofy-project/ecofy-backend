package br.com.ecofy.ms_categorization.adapters.out.outbox;

import br.com.ecofy.ms_categorization.adapters.correlation.CorrelationContext;
import br.com.ecofy.ms_categorization.config.CategorizationProperties;
import br.com.ecofy.ms_categorization.core.domain.outbox.OutboxEvent;
import br.com.ecofy.ms_categorization.core.port.out.OutboxEventPortOut;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// Publica eventos da Outbox no Kafka em transações curtas, com retry, backoff e descarte auditável.
@Slf4j
@Component
public class OutboxPublisher {

    private final OutboxEventPortOut outboxPort;

    // Publica o payload já serializado byte a byte, evitando dupla codificação pelo serializer default.
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CategorizationProperties props;
    private final MeterRegistry meterRegistry;

    // Expõe a idade do evento não publicado mais antigo como gauge de atraso da Outbox.
    private final AtomicLong oldestPendingAgeSeconds = new AtomicLong(0);

    public OutboxPublisher(OutboxEventPortOut outboxPort,
                           @org.springframework.beans.factory.annotation.Qualifier("outboxKafkaTemplate")
                           KafkaTemplate<String, String> outboxKafkaTemplate,
                           CategorizationProperties props,
                           MeterRegistry meterRegistry) {
        this.outboxPort = Objects.requireNonNull(outboxPort, "outboxPort must not be null");
        this.kafkaTemplate = Objects.requireNonNull(outboxKafkaTemplate, "outboxKafkaTemplate must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");

        meterRegistry.gauge("ecofy.categorization.outbox.oldest.pending.age.seconds", oldestPendingAgeSeconds);
    }

    // Processa o ciclo principal de publicação, respeitando o intervalo entre execuções.
    @Scheduled(fixedDelayString = "${ecofy.categorization.outbox.poll-interval:1s}")
    public void publishPendingBatch() {
        Instant now = Instant.now();
        List<OutboxEvent> batch;
        try {
            batch = outboxPort.claimPendingBatch(props.getOutbox().getBatchSize(), now);
        } catch (Exception e) {
            // Banco indisponível: o ciclo seguinte tenta de novo. Nada foi reservado.
            log.error("[OutboxPublisher] - [publishPendingBatch] -> falha ao reservar lote error={}", e.getMessage());
            return;
        }

        if (batch.isEmpty()) {
            refreshOldestPendingGauge(now);
            return;
        }

        log.debug("[OutboxPublisher] - [publishPendingBatch] -> lote reservado size={}", batch.size());

        for (OutboxEvent event : batch) {
            publishOne(event);
        }
        refreshOldestPendingGauge(Instant.now());
    }

    private void publishOne(OutboxEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Envia o envelope JÁ SERIALIZADO na criação: a republicação reenvia
            // exatamente os mesmos bytes — mesmo eventId, mesmo occurredAt (§13.3).
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(event.getTopic(), event.getPartitionKey(), event.getPayload());

            addHeaders(record, event);

            // Confirmação SÍNCRONA do broker (acks=all no yml). PUBLISHED só depois dela.
            kafkaTemplate.send(record).get(props.getOutbox().getPublishTimeout().toMillis(), TimeUnit.MILLISECONDS);

            Instant publishedAt = Instant.now();
            outboxPort.markPublished(event, publishedAt);

            sample.stop(meterRegistry.timer("ecofy.categorization.outbox.publish.duration", "outcome", "success"));
            meterRegistry.counter("ecofy.categorization.outbox.published.total",
                    "event_type", event.getEventType()).increment();

            // Latência criação→publicação: termômetro do atraso da outbox.
            meterRegistry.timer("ecofy.categorization.outbox.lag")
                    .record(Duration.between(event.getCreatedAt(), publishedAt));

            log.info("[OutboxPublisher] - [publishOne] -> Evento da Outbox publicado com sucesso eventId={} eventType={} topic={} attempt={} correlationId={}",
                    event.getId(), event.getEventType(), event.getTopic(), event.getAttempts(), event.getCorrelationId());

        } catch (Exception e) {
            sample.stop(meterRegistry.timer("ecofy.categorization.outbox.publish.duration", "outcome", "failure"));
            handlePublishFailure(event, e);
        }
    }

    private void handlePublishFailure(OutboxEvent event, Exception cause) {
        Instant now = Instant.now();
        // Código controlado, nunca a mensagem crua da exceção (pode ecoar endereço de broker).
        String errorCode = cause.getClass().getSimpleName();

        if (event.hasExhaustedAttempts(props.getOutbox().getMaxAttempts())) {
            outboxPort.markDiscarded(event, errorCode, now);
            meterRegistry.counter("ecofy.categorization.outbox.discarded.total",
                    "event_type", event.getEventType()).increment();

            log.error("[OutboxPublisher] - [handlePublishFailure] -> Evento da Outbox descartado após atingir limite de tentativas eventId={} eventType={} attempts={} "
                            + "errorCode={} correlationId={} (registro preservado para auditoria)",
                    event.getId(), event.getEventType(), event.getAttempts(), errorCode, event.getCorrelationId());
            return;
        }

        Instant nextAttemptAt = now.plus(computeBackoff(event.getAttempts()));
        outboxPort.markFailed(event, errorCode, nextAttemptAt, now);
        meterRegistry.counter("ecofy.categorization.outbox.failed.total",
                "event_type", event.getEventType()).increment();

        log.warn("[OutboxPublisher] - [handlePublishFailure] -> Falha na publicação; nova tentativa agendada eventId={} attempt={} nextAttemptAt={} errorCode={}",
                event.getId(), event.getAttempts(), nextAttemptAt, errorCode);
    }

    // Calcula o backoff exponencial entre tentativas, limitado ao teto configurado.
    private Duration computeBackoff(int attempts) {
        double multiplier = Math.pow(props.getOutbox().getBackoffMultiplier(), Math.max(0, attempts - 1));
        long millis = (long) (props.getOutbox().getInitialBackoff().toMillis() * multiplier);
        long capped = Math.min(millis, props.getOutbox().getMaxBackoff().toMillis());
        return Duration.ofMillis(capped);
    }

    // Aplica os headers Kafka de correlação e identidade, preservados desde a criação do evento.
    private static void addHeaders(ProducerRecord<String, String> record, OutboxEvent event) {
        if (event.getCorrelationId() != null) {
            record.headers().add(header(CorrelationContext.KAFKA_HEADER, event.getCorrelationId()));
        }
        record.headers().add(header("eventId", event.getId().toString()));
        record.headers().add(header("eventType", event.getEventType()));
        record.headers().add(header("eventVersion", String.valueOf(event.getEventVersion())));
        if (event.getCausationId() != null) {
            record.headers().add(header("causationId", event.getCausationId().toString()));
        }
    }

    private static RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }

    // Recupera registros abandonados em PROCESSING, devolvendo-os à fila de publicação.
    @Scheduled(fixedDelayString = "${ecofy.categorization.outbox.processing-timeout:5m}")
    public void recoverStuckProcessing() {
        try {
            int released = outboxPort.releaseStuck(props.getOutbox().getProcessingTimeout(), Instant.now());
            if (released > 0) {
                meterRegistry.counter("ecofy.categorization.outbox.recovered.total").increment(released);
            }
        } catch (Exception e) {
            log.error("[OutboxPublisher] - [recoverStuckProcessing] -> falha error={}", e.getMessage());
        }
    }

    // Remove registros PUBLISHED fora da retenção em lotes limitados, preservando FAILED e DISCARDED.
    @Scheduled(fixedDelayString = "${ecofy.categorization.outbox.cleanup-interval:1h}")
    public void cleanupPublished() {
        try {
            int removed = outboxPort.deletePublishedOlderThan(
                    props.getOutbox().getPublishedRetention(),
                    Instant.now(),
                    props.getOutbox().getCleanupBatchSize());

            if (removed > 0) {
                meterRegistry.counter("ecofy.categorization.outbox.cleaned.total").increment(removed);
                log.info("[OutboxPublisher] - [cleanupPublished] -> {} registros PUBLISHED removidos por retenção", removed);
            }
        } catch (Exception e) {
            log.error("[OutboxPublisher] - [cleanupPublished] -> falha error={}", e.getMessage());
        }
    }

    private void refreshOldestPendingGauge(Instant now) {
        try {
            Instant oldest = outboxPort.oldestPendingCreatedAt();
            oldestPendingAgeSeconds.set(oldest == null ? 0 : Duration.between(oldest, now).getSeconds());
        } catch (Exception e) {
            log.debug("[OutboxPublisher] - [refreshOldestPendingGauge] -> falha error={}", e.getMessage());
        }
    }
}
