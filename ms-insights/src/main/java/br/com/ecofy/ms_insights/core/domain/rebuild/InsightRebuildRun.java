package br.com.ecofy.ms_insights.core.domain.rebuild;

import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// Representa uma execução de rebuild com estado, progresso e cursor estável por data para retomada.
public class InsightRebuildRun {

    private static final Map<RebuildStatus, Set<RebuildStatus>> ALLOWED = Map.of(
            RebuildStatus.PENDING, Set.of(RebuildStatus.RUNNING, RebuildStatus.CANCELLED),
            RebuildStatus.RUNNING, Set.of(RebuildStatus.COMPLETED, RebuildStatus.COMPLETED_WITH_ERRORS,
                    RebuildStatus.FAILED, RebuildStatus.CANCELLED),
            RebuildStatus.COMPLETED, Set.of(),
            RebuildStatus.COMPLETED_WITH_ERRORS, Set.of(),
            RebuildStatus.FAILED, Set.of(),
            RebuildStatus.CANCELLED, Set.of()
    );

    private final UUID id;
    private final UUID userId;
    private final InsightType insightType; // null = todos os tipos aplicáveis
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final PeriodGranularity granularity;
    private final RebuildMode mode;
    private final String idempotencyKey;
    private final String correlationId;
    private final Instant createdAt;

    private RebuildStatus status;
    private LocalDate checkpoint;
    private long processedItems;
    private long generatedInsights;
    private long failedItems;
    private String lastErrorCode;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant updatedAt;

    public InsightRebuildRun(UUID id, UUID userId, InsightType insightType, LocalDate periodStart,
                             LocalDate periodEnd, PeriodGranularity granularity, RebuildMode mode,
                             String idempotencyKey, String correlationId, RebuildStatus status,
                             LocalDate checkpoint, long processedItems, long generatedInsights, long failedItems,
                             String lastErrorCode, Instant createdAt, Instant startedAt, Instant finishedAt,
                             Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.insightType = insightType;
        this.periodStart = Objects.requireNonNull(periodStart, "periodStart must not be null");
        this.periodEnd = Objects.requireNonNull(periodEnd, "periodEnd must not be null");
        this.granularity = Objects.requireNonNull(granularity, "granularity must not be null");
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.correlationId = correlationId;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.checkpoint = checkpoint;
        this.processedItems = processedItems;
        this.generatedInsights = generatedInsights;
        this.failedItems = failedItems;
        this.lastErrorCode = lastErrorCode;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.updatedAt = Objects.requireNonNullElse(updatedAt, createdAt);
    }

    public static InsightRebuildRun createPending(UUID id, UUID userId, InsightType insightType,
                                                  LocalDate periodStart, LocalDate periodEnd,
                                                  PeriodGranularity granularity, RebuildMode mode,
                                                  String idempotencyKey, String correlationId, Instant now) {
        return new InsightRebuildRun(id, userId, insightType, periodStart, periodEnd, granularity, mode,
                idempotencyKey, correlationId, RebuildStatus.PENDING, null, 0, 0, 0, null, now, null, null, now);
    }

    public void start(Instant now) {
        transitionTo(RebuildStatus.RUNNING);
        this.startedAt = now;
        this.updatedAt = now;
    }

    // Avança o cursor após processar um período, contabilizando insights gerados e falhas.
    public void advanceCheckpoint(LocalDate processedUpTo, long generatedDelta, long failedDelta, Instant now) {
        this.checkpoint = processedUpTo;
        this.processedItems++;
        this.generatedInsights += generatedDelta;
        this.failedItems += failedDelta;
        this.updatedAt = now;
    }

    public void complete(Instant now) {
        transitionTo(failedItems > 0 ? RebuildStatus.COMPLETED_WITH_ERRORS : RebuildStatus.COMPLETED);
        this.finishedAt = now;
        this.updatedAt = now;
    }

    public void fail(String errorCode, Instant now) {
        transitionTo(RebuildStatus.FAILED);
        this.lastErrorCode = errorCode;
        this.finishedAt = now;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        transitionTo(RebuildStatus.CANCELLED);
        this.finishedAt = now;
        this.updatedAt = now;
    }

    private void transitionTo(RebuildStatus target) {
        Set<RebuildStatus> allowed = ALLOWED.get(status);
        if (allowed == null || !allowed.contains(target)) {
            throw new IllegalStateException("Insight rebuild " + id + " cannot transition from " + status + " to " + target);
        }
        this.status = target;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public InsightType getInsightType() { return insightType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public PeriodGranularity getGranularity() { return granularity; }
    public RebuildMode getMode() { return mode; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCorrelationId() { return correlationId; }
    public RebuildStatus getStatus() { return status; }
    public LocalDate getCheckpoint() { return checkpoint; }
    public long getProcessedItems() { return processedItems; }
    public long getGeneratedInsights() { return generatedInsights; }
    public long getFailedItems() { return failedItems; }
    public String getLastErrorCode() { return lastErrorCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
