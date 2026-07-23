package br.com.ecofy.ms_insights.adapters.out.outbox;

import br.com.ecofy.ms_insights.adapters.correlation.CorrelationContext;
import br.com.ecofy.ms_insights.adapters.out.messaging.EventTypes;
import br.com.ecofy.ms_insights.adapters.out.messaging.dto.InsightCreatedEvent;
import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.outbox.OutboxEvent;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.port.out.OutboxEventPort;
import br.com.ecofy.ms_insights.core.port.out.PublishInsightCreatedEventPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Registra eventos de criação de insights na Outbox transacional.
@Slf4j
@Component
@Primary
public class InsightCreatedOutboxWriter implements PublishInsightCreatedEventPort {

    private static final String EVENT_TYPE_JSON = "insight.created";

    private final OutboxEventPort outboxPort;
    private final ObjectMapper objectMapper;
    private final InsightsProperties props;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public InsightCreatedOutboxWriter(OutboxEventPort outboxPort,
                                      ObjectMapper objectMapper,
                                      InsightsProperties props,
                                      MeterRegistry meterRegistry,
                                      Clock clock) {
        this.outboxPort = Objects.requireNonNull(outboxPort, "outboxPort must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Converte o insight em evento e o persiste atomicamente na Outbox.
    @Override
    public void publish(Insight insight) {
        Objects.requireNonNull(insight, "insight must not be null");

        var key = Objects.requireNonNull(insight.getKey(), "insight.key must not be null");
        UUID userId = key.userId().value();
        UUID insightId = Objects.requireNonNull(insight.getId(), "insight.id must not be null");

        Instant occurredAt = insight.getCreatedAt() != null
                ? insight.getCreatedAt()
                : Instant.now(clock);
        String correlationId = CorrelationContext.currentCorrelationIdOrGenerate();
        UUID causationId = CorrelationContext.currentCausationId();

        Period period = key.period();
        String periodStart = period != null && period.start() != null
                ? period.start().toString()
                : null;
        String periodEnd = period != null && period.end() != null
                ? period.end().toString()
                : null;
        String insightType = insight.getType() != null
                ? insight.getType().name()
                : null;

        InsightCreatedEvent event = new InsightCreatedEvent(
                insightId.toString(),
                EVENT_TYPE_JSON,
                userId.toString(),
                insightId.toString(),
                insightType,
                insight.getScore(),
                periodStart,
                periodEnd,
                occurredAt,
                insight.getPayload(),
                new InsightCreatedEvent.EventMetadata(
                        insightId.toString(),
                        correlationId,
                        occurredAt,
                        EventTypes.PRODUCER
                )
        );

        String partitionKey = userId.toString();

        OutboxEvent outboxEvent = OutboxEvent.createPending(
                insightId,
                EventTypes.AGGREGATE_TYPE_INSIGHT,
                insightId,
                EventTypes.INSIGHT_CREATED,
                EventTypes.INSIGHT_CREATED_VERSION,
                props.topics().insightCreatedTopic(),
                partitionKey,
                serialize(event),
                correlationId,
                causationId,
                occurredAt
        );

        outboxPort.save(outboxEvent);

        meterRegistry.counter(
                "ecofy.insights.outbox.created.total",
                "event_type",
                EventTypes.INSIGHT_CREATED
        ).increment();

        log.info(
                "[InsightCreatedOutboxWriter] - [publish] -> Evento gravado na Outbox (PENDING) eventId={} userId={} type={} correlationId={}",
                insightId,
                userId,
                insightType,
                correlationId
        );
    }

    // Serializa o evento e propaga falhas para interromper a transação.
    private String serialize(InsightCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize insight.created event",
                    e
            );
        }
    }
}
