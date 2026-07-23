package br.com.ecofy.ms_insights.adapters.out.outbox;

import br.com.ecofy.ms_insights.adapters.correlation.CorrelationContext;
import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.domain.outbox.OutboxEvent;
import br.com.ecofy.ms_insights.core.port.out.OutboxEventPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Qualifier;
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

// Publica eventos da Outbox e gerencia seu ciclo de processamento.
@Slf4j
@Component
public class InsightCreatedOutboxPublisher {

    private final OutboxEventPort outboxPort;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final InsightsProperties props;
    private final MeterRegistry meterRegistry;

    private final AtomicLong oldestPendingAgeSeconds = new AtomicLong(0);

    public InsightCreatedOutboxPublisher(
            OutboxEventPort outboxPort,
            @Qualifier("insightsOutboxKafkaTemplate")
            KafkaTemplate<String, String> outboxKafkaTemplate,
            InsightsProperties props,
            MeterRegistry meterRegistry
    ) {
        this.outboxPort = Objects.requireNonNull(outboxPort, "outboxPort must not be null");
        this.kafkaTemplate = Objects.requireNonNull(outboxKafkaTemplate, "outboxKafkaTemplate must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        meterRegistry.gauge("ecofy.insights.outbox.oldest.pending.age.seconds", oldestPendingAgeSeconds);
    }

    // Reserva e publica um lote de eventos pendentes.
    @Scheduled(fixedDelayString = "${ecofy.insights.outbox.poll-interval:1s}")
    public void publishPendingBatch() {
        Instant now = Instant.now();
        List<OutboxEvent> batch;
        try {
            batch = outboxPort.claimPendingBatch(props.outbox().batchSize(), now);
        } catch (Exception e) {
            log.error(
                    "[InsightCreatedOutboxPublisher] - [publishPendingBatch] -> falha ao reservar lote error={}",
                    e.getMessage()
            );
            return;
        }

        if (batch.isEmpty()) {
            refreshOldestPendingGauge(now);
            return;
        }

        for (OutboxEvent event : batch) {
            publishOne(event);
        }
        refreshOldestPendingGauge(Instant.now());
    }

    // Publica um evento e registra o resultado do processamento.
    private void publishOne(OutboxEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(event.getTopic(), event.getPartitionKey(), event.getPayload());
            addHeaders(record, event);

            kafkaTemplate.send(record).get(
                    props.outbox().publishTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );

            Instant publishedAt = Instant.now();
            outboxPort.markPublished(event, publishedAt);

            sample.stop(meterRegistry.timer(
                    "ecofy.insights.outbox.publish.duration",
                    "outcome",
                    "success"
            ));
            meterRegistry.counter(
                    "ecofy.insights.outbox.published.total",
                    "event_type",
                    event.getEventType()
            ).increment();
            meterRegistry.timer("ecofy.insights.outbox.lag")
                    .record(Duration.between(event.getCreatedAt(), publishedAt));

            log.info(
                    "[InsightCreatedOutboxPublisher] - [publishOne] -> Evento da Outbox publicado com sucesso eventId={} topic={} attempt={} correlationId={}",
                    event.getId(),
                    event.getTopic(),
                    event.getAttempts(),
                    event.getCorrelationId()
            );

        } catch (Exception e) {
            sample.stop(meterRegistry.timer(
                    "ecofy.insights.outbox.publish.duration",
                    "outcome",
                    "failure"
            ));
            handleFailure(event, e);
        }
    }

    // Agenda uma nova tentativa ou descarta eventos que excederam o limite.
    private void handleFailure(OutboxEvent event, Exception cause) {
        Instant now = Instant.now();
        String errorCode = cause.getClass().getSimpleName();

        if (event.hasExhaustedAttempts(props.outbox().maxAttempts())) {
            outboxPort.markDiscarded(event, errorCode, now);
            meterRegistry.counter(
                    "ecofy.insights.outbox.discarded.total",
                    "event_type",
                    event.getEventType()
            ).increment();
            log.error(
                    "[InsightCreatedOutboxPublisher] - [handleFailure] -> Evento da Outbox descartado após atingir limite de tentativas eventId={} attempts={} errorCode={} "
                            + "(registro preservado para auditoria)",
                    event.getId(),
                    event.getAttempts(),
                    errorCode
            );
            return;
        }

        Instant nextAttemptAt = now.plus(computeBackoff(event.getAttempts()));
        outboxPort.markFailed(event, errorCode, nextAttemptAt, now);
        meterRegistry.counter(
                "ecofy.insights.outbox.failed.total",
                "event_type",
                event.getEventType()
        ).increment();
        log.warn(
                "[InsightCreatedOutboxPublisher] - [handleFailure] -> Falha na publicação; nova tentativa agendada eventId={} attempt={} nextAttemptAt={} errorCode={}",
                event.getId(),
                event.getAttempts(),
                nextAttemptAt,
                errorCode
        );
    }

    // Calcula o intervalo progressivo entre tentativas de publicação.
    private Duration computeBackoff(int attempts) {
        double multiplier = Math.pow(
                props.outbox().backoffMultiplier(),
                Math.max(0, attempts - 1)
        );
        long millis = (long) (
                props.outbox().initialBackoff().toMillis() * multiplier
        );
        long capped = Math.min(
                millis,
                props.outbox().maxBackoff().toMillis()
        );
        return Duration.ofMillis(capped);
    }

    // Propaga os metadados de rastreamento e identificação do evento.
    private static void addHeaders(
            ProducerRecord<String, String> record,
            OutboxEvent event
    ) {
        if (event.getCorrelationId() != null) {
            record.headers().add(
                    header(CorrelationContext.KAFKA_HEADER, event.getCorrelationId())
            );
        }
        record.headers().add(header("eventId", event.getId().toString()));
        record.headers().add(header("eventType", event.getEventType()));
        record.headers().add(
                header("eventVersion", String.valueOf(event.getEventVersion()))
        );
        if (event.getCausationId() != null) {
            record.headers().add(
                    header("causationId", event.getCausationId().toString())
            );
        }
    }

    private static RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }

    // Recupera eventos abandonados durante o processamento.
    @Scheduled(fixedDelayString = "${ecofy.insights.outbox.processing-timeout:5m}")
    public void recoverStuckProcessing() {
        try {
            int released = outboxPort.releaseStuck(
                    props.outbox().processingTimeout(),
                    Instant.now()
            );
            if (released > 0) {
                meterRegistry.counter(
                        "ecofy.insights.outbox.recovered.total"
                ).increment(released);
            }
        } catch (Exception e) {
            log.error(
                    "[InsightCreatedOutboxPublisher] - [recoverStuckProcessing] -> falha error={}",
                    e.getMessage()
            );
        }
    }

    // Remove eventos publicados que excederam o período de retenção.
    @Scheduled(fixedDelayString = "${ecofy.insights.outbox.cleanup-interval:1h}")
    public void cleanupPublished() {
        try {
            int removed = outboxPort.deletePublishedOlderThan(
                    props.outbox().publishedRetention(),
                    Instant.now(),
                    props.outbox().cleanupBatchSize()
            );
            if (removed > 0) {
                meterRegistry.counter(
                        "ecofy.insights.outbox.cleaned.total"
                ).increment(removed);
                log.info(
                        "[InsightCreatedOutboxPublisher] - [cleanupPublished] -> {} registros PUBLISHED removidos",
                        removed
                );
            }
        } catch (Exception e) {
            log.error(
                    "[InsightCreatedOutboxPublisher] - [cleanupPublished] -> falha error={}",
                    e.getMessage()
            );
        }
    }

    // Atualiza a idade do evento pendente mais antigo.
    private void refreshOldestPendingGauge(Instant now) {
        try {
            Instant oldest = outboxPort.oldestPendingCreatedAt();
            oldestPendingAgeSeconds.set(
                    oldest == null
                            ? 0
                            : Duration.between(oldest, now).getSeconds()
            );
        } catch (Exception e) {
            log.debug(
                    "[InsightCreatedOutboxPublisher] - [refreshOldestPendingGauge] -> falha error={}",
                    e.getMessage()
            );
        }
    }
}
