package br.com.ecofy.ms_insights.core.domain;

import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.valueobject.InsightKey;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class Insight {

    private final UUID id;
    private final InsightKey key;
    private final InsightType type;
    private final int score;
    private final String title;
    private final String summary;
    private final Map<String, Object> payload;
    private final Instant createdAt;

    public Insight(UUID id,
                   InsightKey key,
                   InsightType type,
                   int score,
                   String title,
                   String summary,
                   Map<String, Object> payload,
                   Instant createdAt) {
        this.id = id;
        this.key = key;
        this.type = type;
        this.score = score;
        this.title = title;
        this.summary = summary;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public InsightKey getKey() { return key; }
    public InsightType getType() { return type; }
    public int getScore() { return score; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public Map<String, Object> getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }

}
