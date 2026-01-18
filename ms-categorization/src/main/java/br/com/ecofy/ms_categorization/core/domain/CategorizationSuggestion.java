package br.com.ecofy.ms_categorization.core.domain;

import br.com.ecofy.ms_categorization.core.domain.enums.SuggestionStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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

    // Representa uma sugestão/decisão de categorização (auto/manual/unmatched) associada a uma transação, com score e metadados para auditoria.
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

    // Retorna o identificador único da sugestão.
    public UUID getId() {
        return id;
    }

    // Retorna o id da transação a que esta sugestão se refere.
    public UUID getTransactionId() {
        return transactionId;
    }

    // Retorna o id da categoria sugerida/aplicada (pode ser null em UNMATCHED).
    public UUID getCategoryId() {
        return categoryId;
    }

    // Retorna o id da regra que originou a sugestão (pode ser null em categorização manual ou unmatched).
    public UUID getRuleId() {
        return ruleId;
    }

    // Retorna o status da sugestão (ex.: APPLIED_AUTO, APPLIED_MANUAL, UNMATCHED).
    public SuggestionStatus getStatus() {
        return status;
    }

    // Retorna o score atribuído à sugestão para suporte à decisão/telemetria.
    public int getScore() {
        return score;
    }

    // Retorna a justificativa textual (opcional) associada à sugestão.
    public String getRationale() {
        return rationale;
    }

    // Retorna o timestamp de criação para rastreabilidade/auditoria.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Retorna o timestamp da última atualização para rastreabilidade/auditoria.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Compara sugestões por valor (campos relevantes do objeto) para consistência em coleções e testes.
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

    // Gera hash consistente com equals para uso em estruturas baseadas em hashing.
    @Override
    public int hashCode() {
        return Objects.hash(id, transactionId, categoryId, ruleId, status, score, rationale, createdAt, updatedAt);
    }

    // Fornece uma representação textual completa da sugestão para logs e debug.
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
