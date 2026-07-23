package br.com.ecofy.ms_insights.adapters.out.persistence.entity;

import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.rebuild.RebuildMode;
import br.com.ecofy.ms_insights.core.domain.rebuild.RebuildStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "insight_rebuild_run")
public class InsightRebuildRunEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "insight_type", length = 40)
    private InsightType insightType;

    @Column(name = "period_start", nullable = false, updatable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false, updatable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "granularity", length = 10, nullable = false, updatable = false)
    private PeriodGranularity granularity;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 20, nullable = false, updatable = false)
    private RebuildMode mode;

    @Column(name = "idempotency_key", length = 300, nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "correlation_id", length = 128, updatable = false)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private RebuildStatus status;

    @Column(name = "checkpoint")
    private LocalDate checkpoint;

    @Column(name = "processed_items", nullable = false)
    private long processedItems;

    @Column(name = "generated_insights", nullable = false)
    private long generatedInsights;

    @Column(name = "failed_items", nullable = false)
    private long failedItems;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
