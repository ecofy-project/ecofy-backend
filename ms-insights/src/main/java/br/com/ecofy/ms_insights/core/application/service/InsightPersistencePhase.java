package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.adapters.out.cache.CacheInvalidator;
import br.com.ecofy.ms_insights.core.application.result.*;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.MetricSnapshot;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.MetricType;
import br.com.ecofy.ms_insights.core.domain.valueobject.InsightKey;
import br.com.ecofy.ms_insights.core.domain.valueobject.Money;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import br.com.ecofy.ms_insights.core.port.out.LoadGoalsPort;
import br.com.ecofy.ms_insights.core.port.out.PublishInsightCreatedEventPort;
import br.com.ecofy.ms_insights.core.port.out.SaveInsightPort;
import br.com.ecofy.ms_insights.core.port.out.SaveMetricSnapshotPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Persiste atomicamente métricas, insights e eventos da Outbox.
@Slf4j
@Component
public class InsightPersistencePhase {

    private final SaveInsightPort saveInsightPort;
    private final SaveMetricSnapshotPort saveMetricSnapshotPort;
    private final LoadGoalsPort loadGoalsPort;
    private final PublishInsightCreatedEventPort publishInsightCreatedEventPort;
    private final CacheInvalidator cacheInvalidator;
    private final MeterRegistry meterRegistry;

    public InsightPersistencePhase(
            SaveInsightPort saveInsightPort,
            SaveMetricSnapshotPort saveMetricSnapshotPort,
            LoadGoalsPort loadGoalsPort,
            PublishInsightCreatedEventPort publishInsightCreatedEventPort,
            CacheInvalidator cacheInvalidator,
            MeterRegistry meterRegistry
    ) {
        this.saveInsightPort = Objects.requireNonNull(
                saveInsightPort,
                "saveInsightPort must not be null"
        );
        this.saveMetricSnapshotPort = Objects.requireNonNull(
                saveMetricSnapshotPort,
                "saveMetricSnapshotPort must not be null"
        );
        this.loadGoalsPort = Objects.requireNonNull(
                loadGoalsPort,
                "loadGoalsPort must not be null"
        );
        this.publishInsightCreatedEventPort = Objects.requireNonNull(
                publishInsightCreatedEventPort,
                "publishInsightCreatedEventPort must not be null"
        );
        this.cacheInvalidator = Objects.requireNonNull(
                cacheInvalidator,
                "cacheInvalidator must not be null"
        );
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null"
        );
    }

    // Transporta os dados preparados para a etapa de persistência.
    public record PreparedInsight(
            UserId userId,
            Period period,
            InsightType insightType,
            String currency,
            long totalSpentCents,
            long totalIncomeCents,
            int score,
            int minScoreToPublish,
            String title,
            String summary,
            Map<String, Object> payload,
            java.time.Instant now
    ) {
    }

    // Persiste os dados gerados e publica o evento quando elegível.
    @Transactional
    public InsightsBundleResult persist(PreparedInsight p) {
        Objects.requireNonNull(p, "prepared must not be null");

        MetricSnapshot spentSnap = saveMetricSnapshotPort.save(
                new MetricSnapshot(
                        UUID.randomUUID(),
                        p.userId(),
                        p.period(),
                        MetricType.TOTAL_SPENT,
                        Money.ofCents(
                                p.totalSpentCents(),
                                p.currency()
                        ),
                        p.now()
                )
        );

        MetricSnapshot incomeSnap = saveMetricSnapshotPort.save(
                new MetricSnapshot(
                        UUID.randomUUID(),
                        p.userId(),
                        p.period(),
                        MetricType.INCOME,
                        Money.ofCents(
                                p.totalIncomeCents(),
                                p.currency()
                        ),
                        p.now()
                )
        );

        InsightKey key = new InsightKey(
                p.userId(),
                p.insightType(),
                p.period()
        );
        Insight insight = new Insight(
                UUID.randomUUID(),
                key,
                p.insightType(),
                p.score(),
                p.title(),
                p.summary(),
                p.payload(),
                p.now()
        );

        Insight saved = saveInsightPort.save(insight);

        boolean shouldPublish =
                saved.getScore() >= p.minScoreToPublish();
        if (shouldPublish) {
            publishInsightCreatedEventPort.publish(saved);
        }

        meterRegistry.counter(
                "ecofy.insights.generated.total",
                "insight_type",
                saved.getType().name(),
                "published",
                Boolean.toString(shouldPublish)
        ).increment();

        cacheInvalidator.evictUser(p.userId().value());

        var goals = loadGoalsPort.findByUserId(
                p.userId().value()
        );

        log.info(
                "[InsightPersistencePhase] - [persist] -> insightId={} userId={} type={} score={} shouldPublish={} minScore={}",
                saved.getId(),
                p.userId().value(),
                saved.getType(),
                saved.getScore(),
                shouldPublish,
                p.minScoreToPublish()
        );

        return new InsightsBundleResult(
                List.of(toInsightResult(saved)),
                List.of(
                        toMetricResult(spentSnap),
                        toMetricResult(incomeSnap)
                ),
                goals.stream()
                        .map(GoalService::toResult)
                        .toList()
        );
    }

    private static InsightResult toInsightResult(Insight i) {
        return new InsightResult(
                i.getId(),
                i.getKey().userId().value(),
                i.getType(),
                i.getScore(),
                i.getTitle(),
                i.getSummary(),
                i.getPayload(),
                i.getCreatedAt()
        );
    }

    private static MetricSnapshotResult toMetricResult(
            MetricSnapshot s
    ) {
        return new MetricSnapshotResult(
                s.getId(),
                s.getUserId().value(),
                s.getMetricType(),
                s.getValue().cents(),
                s.getValue().currency(),
                s.getCreatedAt()
        );
    }
}
