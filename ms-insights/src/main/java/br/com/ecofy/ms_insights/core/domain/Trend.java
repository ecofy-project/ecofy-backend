package br.com.ecofy.ms_insights.core.domain;

import br.com.ecofy.ms_insights.core.domain.enums.TrendType;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Trend {

    private final UUID id;
    private final UserId userId;
    private final Period period;
    private final TrendType type;
    private final List<Long> seriesCents;
    private final String currency;
    private final Instant createdAt;

    public Trend(UUID id, UserId userId, Period period, TrendType type, List<Long> seriesCents, String currency, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.period = period;
        this.type = type;
        this.seriesCents = seriesCents;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public Period getPeriod() { return period; }
    public TrendType getType() { return type; }
    public List<Long> getSeriesCents() { return seriesCents; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }

}
