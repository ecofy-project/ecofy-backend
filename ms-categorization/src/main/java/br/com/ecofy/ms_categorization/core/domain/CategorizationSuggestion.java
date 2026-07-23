package br.com.ecofy.ms_categorization.core.domain;

import br.com.ecofy.ms_categorization.core.domain.enums.SuggestionStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Representa uma decisão de categorização associada a uma transação.
public final class CategorizationSuggestion {

    private final UUID id;
    private final UUID transactionId;
    private final UUID categoryId;
    private final UUID ruleId;
    private final SuggestionStatus status;
    private final int score;
    private final String rationale;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CategorizationSuggestion(
            UUID id,
            UUID transactionId,
            UUID categoryId,
            UUID ruleId,
            SuggestionStatus status,
            int score,
            String rationale,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        this.categoryId = categoryId;
        this.ruleId = ruleId;
        this.score = score;
        this.rationale = rationale;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public int getScore() {
        return score;
    }

    public String getRationale() {
        return rationale;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategorizationSuggestion that)) return false;
        return score == that.score &&
                id.equals(that.id) &&
                transactionId.equals(that.transactionId) &&
                Objects.equals(categoryId, that.categoryId) &&
                Objects.equals(ruleId, that.ruleId) &&
                status == that.status &&
                Objects.equals(rationale, that.rationale) &&
                createdAt.equals(that.createdAt) &&
                updatedAt.equals(that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transactionId, categoryId, ruleId, status, score, rationale, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "CategorizationSuggestion[" +
                "id=" + id +
                ", transactionId=" + transactionId +
                ", categoryId=" + categoryId +
                ", ruleId=" + ruleId +
                ", status=" + status +
                ", score=" + score +
                ", rationale=" + rationale +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ']';
    }
}
