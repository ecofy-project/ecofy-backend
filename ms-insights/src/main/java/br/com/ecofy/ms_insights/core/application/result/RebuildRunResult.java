package br.com.ecofy.ms_insights.core.application.result;

import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.rebuild.InsightRebuildRun;
import br.com.ecofy.ms_insights.core.domain.rebuild.RebuildMode;
import br.com.ecofy.ms_insights.core.domain.rebuild.RebuildStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Expõe o estado e o progresso de uma execução de rebuild.
public record RebuildRunResult(
        UUID id,
        UUID userId,
        InsightType insightType,
        LocalDate periodStart,
        LocalDate periodEnd,
        PeriodGranularity granularity,
        RebuildMode mode,
        RebuildStatus status,
        LocalDate checkpoint,
        long processedItems,
        long generatedInsights,
        long failedItems,
        String lastErrorCode,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {
    public static RebuildRunResult from(InsightRebuildRun r) {
        return new RebuildRunResult(
                r.getId(), r.getUserId(), r.getInsightType(), r.getPeriodStart(), r.getPeriodEnd(),
                r.getGranularity(), r.getMode(), r.getStatus(), r.getCheckpoint(), r.getProcessedItems(),
                r.getGeneratedInsights(), r.getFailedItems(), r.getLastErrorCode(),
                r.getCreatedAt(), r.getStartedAt(), r.getFinishedAt());
    }
}
