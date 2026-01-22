package br.com.ecofy.ms_insights.core.domain;

import br.com.ecofy.ms_insights.core.domain.enums.MetricType;
import br.com.ecofy.ms_insights.core.domain.valueobject.Money;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;

import java.time.Instant;
import java.util.UUID;

public class MetricSnapshot {

    private final UUID id;
    private final UserId userId;
    private final Period period;
    private final MetricType metricType;
    private final Money value;
    private final Instant createdAt;

    public MetricSnapshot(UUID id, UserId userId, Period period, MetricType metricType, Money value, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.period = period;
        this.metricType = metricType;
        this.value = value;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public Period getPeriod() { return period; }
    public MetricType getMetricType() { return metricType; }
    public Money getValue() { return value; }
    public Instant getCreatedAt() { return createdAt; }

}
