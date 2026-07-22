package br.com.ecofy.ms_budgeting.adapters.out.outbox;

import br.com.ecofy.ms_budgeting.adapters.correlation.CorrelationContext;
import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.domain.outbox.OutboxEvent;
import br.com.ecofy.ms_budgeting.core.port.out.OutboxEventPort;
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

@Slf4j
@Component
// Publica e mantém eventos da Outbox destinados ao Kafka.
public class BudgetAlertOutboxPublisher {

    private final OutboxEventPort outboxPort;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BudgetingProperties props;
    private final MeterRegistry meterRegistry;

    private final AtomicLong oldestPendingAgeSeconds = new AtomicLong(0);

    public BudgetAlertOutboxPublisher(
            OutboxEventPort outboxPort,
            @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> outboxKafkaTemplate,
            BudgetingProperties props,
            MeterRegistry meterRegistry
    ) {
        this.outboxPort = Objects.requireNonNull(outboxPort, "outboxPort must not be null");
        this.kafkaTemplate = Objects.requireNonNull(outboxKafkaTemplate, "outboxKafkaTemplate must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        meterRegistry.gauge("ecofy.budgeting.outbox.oldest.pending.age.seconds", oldestPendingAgeSeconds);
    }

    @Scheduled(fixedDelayString = "${ecofy.budgeting.outbox.poll-interval:1s}")
    // Publica em lote os eventos pendentes da Outbox.
    public void publishPendingBatch() {
        Instant now = Instant.now();
        List<OutboxEvent> batch;

        try {
            batch = outboxPort.claimPendingBatch(props.outbox().batchSize(), now);
        } catch (Exception e) {
            log.error(
                    "[BudgetAlertOutboxPublisher] - [publishPendingBatch] -> falha ao reservar lote error={}",
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

    private void publishOne(OutboxEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(event.getTopic(), event.getPartitionKey(), event.getPayload());

            addHeaders(record, event);

            kafkaTemplate.send(record)
                    .get(props.outbox().publishTimeout().toMillis(), TimeUnit.MILLISECONDS);

            Instant publishedAt = Instant.now();
            outboxPort.markPublished(event, publishedAt);

            sample.stop(meterRegistry.timer(
                    "ecofy.budgeting.outbox.publish.duration",
                    "outcome",
                    "success"
            ));

            meterRegistry.counter(
                    "ecofy.budgeting.outbox.published.total",
                    "event_type",
                    event.getEventType()
            ).increment();

            meterRegistry.timer("ecofy.budgeting.outbox.lag")
                    .record(Duration.between(event.getCreatedAt(), publishedAt));

            log.info(
                    "[BudgetAlertOutboxPublisher] - [publishOne] -> Evento da Outbox publicado com sucesso eventId={} topic={} attempt={} correlationId={}",
                    event.getId(),
                    event.getTopic(),
                    event.getAttempts(),
                    event.getCorrelationId()
            );
        } catch (Exception e) {
            sample.stop(meterRegistry.timer(
                    "ecofy.budgeting.outbox.publish.duration",
                    "outcome",
                    "failure"
            ));

            handleFailure(event, e);
        }
    }

    private void handleFailure(OutboxEvent event, Exception cause) {
        Instant now = Instant.now();
        String errorCode = cause.getClass().getSimpleName();

        if (event.hasExhaustedAttempts(props.outbox().maxAttempts())) {
            outboxPort.markDiscarded(event, errorCode, now);

            meterRegistry.counter(
                    "ecofy.budgeting.outbox.discarded.total",
                    "event_type",
                    event.getEventType()
            ).increment();

            log.error(
                    "[BudgetAlertOutboxPublisher] - [handleFailure] -> Evento da Outbox descartado após atingir limite de tentativas eventId={} attempts={} errorCode={} "
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
                "ecofy.budgeting.outbox.failed.total",
                "event_type",
                event.getEventType()
        ).increment();

        log.warn(
                "[BudgetAlertOutboxPublisher] - [handleFailure] -> Falha na publicação; nova tentativa agendada eventId={} attempt={} nextAttemptAt={} errorCode={}",
                event.getId(),
                event.getAttempts(),
                nextAttemptAt,
                errorCode
        );
    }

    private Duration computeBackoff(int attempts) {
        double multiplier = Math.pow(
                props.outbox().backoffMultiplier(),
                Math.max(0, attempts - 1)
        );

        long millis = (long) (props.outbox().initialBackoff().toMillis() * multiplier);
        long capped = Math.min(millis, props.outbox().maxBackoff().toMillis());

        return Duration.ofMillis(capped);
    }

    private static void addHeaders(ProducerRecord<String, String> record, OutboxEvent event) {
        if (event.getCorrelationId() != null) {
            record.headers().add(header(
                    CorrelationContext.KAFKA_HEADER,
                    event.getCorrelationId()
            ));
        }

        record.headers().add(header("eventId", event.getId().toString()));
        record.headers().add(header("eventType", event.getEventType()));
        record.headers().add(header("eventVersion", String.valueOf(event.getEventVersion())));

        if (event.getCausationId() != null) {
            record.headers().add(header(
                    "causationId",
                    event.getCausationId().toString()
            ));
        }
    }

    private static RecordHeader header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }

    @Scheduled(fixedDelayString = "${ecofy.budgeting.outbox.processing-timeout:5m}")
    // Recupera eventos retidos em processamento após interrupções do publicador.
    public void recoverStuckProcessing() {
        try {
            int released = outboxPort.releaseStuck(
                    props.outbox().processingTimeout(),
                    Instant.now()
            );

            if (released > 0) {
                meterRegistry.counter("ecofy.budgeting.outbox.recovered.total")
                        .increment(released);
            }
        } catch (Exception e) {
            log.error(
                    "[BudgetAlertOutboxPublisher] - [recoverStuckProcessing] -> falha error={}",
                    e.getMessage()
            );
        }
    }

    @Scheduled(fixedDelayString = "${ecofy.budgeting.outbox.cleanup-interval:1h}")
    // Remove eventos publicados que excederam o período de retenção.
    public void cleanupPublished() {
        try {
            int removed = outboxPort.deletePublishedOlderThan(
                    props.outbox().publishedRetention(),
                    Instant.now(),
                    props.outbox().cleanupBatchSize()
            );

            if (removed > 0) {
                meterRegistry.counter("ecofy.budgeting.outbox.cleaned.total")
                        .increment(removed);

                log.info(
                        "[BudgetAlertOutboxPublisher] - [cleanupPublished] -> {} registros PUBLISHED removidos",
                        removed
                );
            }
        } catch (Exception e) {
            log.error(
                    "[BudgetAlertOutboxPublisher] - [cleanupPublished] -> falha error={}",
                    e.getMessage()
            );
        }
    }

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
                    "[BudgetAlertOutboxPublisher] - [refreshOldestPendingGauge] -> falha error={}",
                    e.getMessage()
            );
        }
    }
}
