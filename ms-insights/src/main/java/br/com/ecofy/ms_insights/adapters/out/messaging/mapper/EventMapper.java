package br.com.ecofy.ms_insights.adapters.out.messaging.mapper;

import br.com.ecofy.ms_insights.adapters.out.messaging.dto.InsightCreatedEvent;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Converte insights do domínio em eventos de integração.
public final class EventMapper {

    private static final String EVENT_TYPE = "insight.created";
    private static final String SOURCE = "ms-insights";

    private EventMapper() {
    }

    public static InsightCreatedEvent toCreatedEvent(Insight insight) {
        return toCreatedEvent(insight, Clock.systemUTC());
    }

    public static InsightCreatedEvent toCreatedEvent(Insight insight, Clock clock) {
        Objects.requireNonNull(insight, "insight must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        var key = Objects.requireNonNull(
                insight.getKey(),
                "insight.key must not be null"
        );
        String userId = key.userId().value().toString();
        String insightId = Objects.requireNonNull(
                insight.getId(),
                "insight.id must not be null"
        ).toString();

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

        Instant occurredAt = Instant.now(clock);
        String eventId = UUID.randomUUID().toString();

        var metadata = new InsightCreatedEvent.EventMetadata(
                eventId,
                null,
                occurredAt,
                SOURCE
        );

        return new InsightCreatedEvent(
                eventId,
                EVENT_TYPE,
                userId,
                insightId,
                insightType,
                insight.getScore(),
                periodStart,
                periodEnd,
                occurredAt,
                insight.getPayload(),
                metadata
        );
    }
}
