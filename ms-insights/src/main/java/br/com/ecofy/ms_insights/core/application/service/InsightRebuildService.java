package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.adapters.correlation.CorrelationContext;
import br.com.ecofy.ms_insights.adapters.out.cache.CacheInvalidator;
import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.application.command.GenerateInsightsCommand;
import br.com.ecofy.ms_insights.core.application.command.RebuildInsightsCommand;
import br.com.ecofy.ms_insights.core.application.result.RebuildRunResult;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_insights.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_insights.core.domain.exception.RebuildRunNotFoundException;
import br.com.ecofy.ms_insights.core.domain.rebuild.InsightRebuildRun;
import br.com.ecofy.ms_insights.core.domain.rebuild.RebuildMode;
import br.com.ecofy.ms_insights.core.port.in.GenerateInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.in.RebuildInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.in.RebuildRunQueryUseCase;
import br.com.ecofy.ms_insights.core.port.out.PageResult;
import br.com.ecofy.ms_insights.core.port.out.RebuildRunPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Reconstrói insights em lotes idempotentes com checkpoint por período.
@Slf4j
@Service
public class InsightRebuildService implements
        RebuildInsightsUseCase,
        RebuildRunQueryUseCase {

    private static final String RULE_VERSION = "v1";
    private static final InsightType DEFAULT_TYPE =
            InsightType.SPENDING_BREAKDOWN;

    private final GenerateInsightsUseCase generateInsightsUseCase;
    private final RebuildRunPort rebuildRunPort;
    private final InsightsProperties properties;
    private final CacheInvalidator cacheInvalidator;
    private final MeterRegistry meterRegistry;
    private final java.time.Clock clock;

    public InsightRebuildService(
            GenerateInsightsUseCase generateInsightsUseCase,
            RebuildRunPort rebuildRunPort,
            InsightsProperties properties,
            CacheInvalidator cacheInvalidator,
            MeterRegistry meterRegistry,
            java.time.Clock clock
    ) {
        this.generateInsightsUseCase = Objects.requireNonNull(
                generateInsightsUseCase,
                "generateInsightsUseCase must not be null"
        );
        this.rebuildRunPort = Objects.requireNonNull(
                rebuildRunPort,
                "rebuildRunPort must not be null"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null"
        );
        this.cacheInvalidator = Objects.requireNonNull(
                cacheInvalidator,
                "cacheInvalidator must not be null"
        );
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock must not be null"
        );
    }

    // Coordena a reconstrução e reaproveita execuções ativas equivalentes.
    @Override
    public RebuildRunResult rebuild(RebuildInsightsCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");
        UUID userId = Objects.requireNonNull(
                cmd.userId(),
                "userId must not be null"
        );
        LocalDate start = Objects.requireNonNull(
                cmd.periodStart(),
                "periodStart must not be null"
        );
        LocalDate end = Objects.requireNonNull(
                cmd.periodEnd(),
                "periodEnd must not be null"
        );
        PeriodGranularity granularity = cmd.granularity() != null
                ? cmd.granularity()
                : PeriodGranularity.MONTH;
        InsightType type = cmd.insightType() != null
                ? cmd.insightType()
                : DEFAULT_TYPE;
        RebuildMode mode = cmd.mode() != null
                ? cmd.mode()
                : RebuildMode.MISSING;

        validateRange(start, end);

        String idemKey = idempotencyKey(
                userId,
                start,
                end,
                granularity,
                type
        );

        var active = rebuildRunPort.findActiveByIdempotencyKey(idemKey);
        if (active.isPresent()) {
            log.info(
                    "[InsightRebuildService] - [rebuild] -> REUSING active run runId={} idemKey={}",
                    active.get().getId(),
                    idemKey
            );
            return RebuildRunResult.from(active.get());
        }

        Instant now = Instant.now(clock);
        String correlationId =
                CorrelationContext.currentCorrelationIdOrGenerate();

        InsightRebuildRun run = InsightRebuildRun.createPending(
                UUID.randomUUID(),
                userId,
                type,
                start,
                end,
                granularity,
                mode,
                idemKey,
                correlationId,
                now
        );
        run.start(now);
        run = rebuildRunPort.save(run);

        meterRegistry.counter(
                "ecofy.insights.rebuild.started.total",
                "mode",
                mode.name()
        ).increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            processBuckets(run, userId, granularity);
            run.complete(Instant.now(clock));
            run = rebuildRunPort.save(run);

            cacheInvalidator.evictUser(userId);

            sample.stop(meterRegistry.timer(
                    "ecofy.insights.rebuild.duration",
                    "outcome",
                    "success"
            ));
            meterRegistry.counter(
                    "ecofy.insights.rebuild.completed.total",
                    "status",
                    run.getStatus().name()
            ).increment();
            meterRegistry.counter(
                    "ecofy.insights.rebuild.processed.total"
            ).increment(run.getProcessedItems());

            log.info(
                    "[InsightRebuildService] - [rebuild] -> DONE runId={} status={} processed={} generated={} failed={}",
                    run.getId(),
                    run.getStatus(),
                    run.getProcessedItems(),
                    run.getGeneratedInsights(),
                    run.getFailedItems()
            );
            return RebuildRunResult.from(run);

        } catch (Exception fatal) {
            run.fail(
                    fatal.getClass().getSimpleName(),
                    Instant.now(clock)
            );
            rebuildRunPort.save(run);
            sample.stop(meterRegistry.timer(
                    "ecofy.insights.rebuild.duration",
                    "outcome",
                    "failure"
            ));
            meterRegistry.counter(
                    "ecofy.insights.rebuild.failed.total"
            ).increment();
            log.error(
                    "[InsightRebuildService] - [rebuild] -> Falha no rebuild runId={} error={}",
                    run.getId(),
                    fatal.getMessage(),
                    fatal
            );
            return RebuildRunResult.from(run);
        }
    }

    // Processa os períodos sequencialmente e persiste checkpoints em lotes.
    private void processBuckets(
            InsightRebuildRun run,
            UUID userId,
            PeriodGranularity granularity
    ) {
        int batchSize = properties.rebuild() != null
                ? properties.rebuild().batchSize()
                : 500;
        int flushEvery = Math.max(1, batchSize);

        LocalDate cursor = run.getCheckpoint() != null
                ? nextBucketStart(
                run.getCheckpoint(),
                granularity
        )
                : run.getPeriodStart();
        int sinceFlush = 0;

        while (!cursor.isAfter(run.getPeriodEnd())) {
            LocalDate bucketStart = cursor;
            LocalDate bucketEnd = bucketEnd(
                    bucketStart,
                    granularity,
                    run.getPeriodEnd()
            );

            long generatedDelta = 0;
            long failedDelta = 0;
            try {
                generateInsightsUseCase.generate(
                        new GenerateInsightsCommand(
                                userId,
                                bucketStart,
                                bucketEnd,
                                granularity,
                                null
                        )
                );
                generatedDelta = 1;
            } catch (IdempotencyViolationException alreadyDone) {
                log.debug(
                        "[InsightRebuildService] - [processBuckets] -> skip existing period start={} end={}",
                        bucketStart,
                        bucketEnd
                );
            } catch (Exception perPeriod) {
                failedDelta = 1;
                run.advanceCheckpoint(
                        bucketStart,
                        0,
                        1,
                        Instant.now(clock)
                );
                log.warn(
                        "[InsightRebuildService] - [processBuckets] -> period FAILED start={} end={} error={}",
                        bucketStart,
                        bucketEnd,
                        perPeriod.getMessage()
                );
                cursor = nextBucketStart(
                        bucketStart,
                        granularity
                );
                if (++sinceFlush >= flushEvery) {
                    rebuildRunPort.save(run);
                    sinceFlush = 0;
                }
                continue;
            }

            run.advanceCheckpoint(
                    bucketStart,
                    generatedDelta,
                    failedDelta,
                    Instant.now(clock)
            );
            cursor = nextBucketStart(
                    bucketStart,
                    granularity
            );

            if (++sinceFlush >= flushEvery) {
                rebuildRunPort.save(run);
                sinceFlush = 0;
            }
        }
    }

    @Override
    public RebuildRunResult getStatus(UUID runId) {
        return rebuildRunPort.findById(runId)
                .map(RebuildRunResult::from)
                .orElseThrow(() ->
                        new RebuildRunNotFoundException(runId)
                );
    }

    @Override
    public PageResult<RebuildRunResult> listByUser(
            UUID userId,
            int page,
            int size
    ) {
        var runs = rebuildRunPort
                .findByUserId(userId, page, size)
                .stream()
                .map(RebuildRunResult::from)
                .toList();
        long total = rebuildRunPort.countByUserId(userId);
        return new PageResult<>(
                runs,
                page,
                size,
                total
        );
    }

    // Valida a ordem e o limite máximo do período solicitado.
    private void validateRange(
            LocalDate start,
            LocalDate end
    ) {
        if (end.isBefore(start)) {
            throw new BusinessValidationException(
                    "periodEnd must be >= periodStart"
            );
        }

        int maxDays = properties.rebuild() != null
                ? properties.rebuild().maxPeriodDays()
                : 365;
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                start,
                end
        );

        if (days > maxDays) {
            throw new BusinessValidationException(
                    "rebuild period too large: "
                            + days
                            + " days (max "
                            + maxDays
                            + ")"
            );
        }
    }

    private static String idempotencyKey(
            UUID userId,
            LocalDate start,
            LocalDate end,
            PeriodGranularity g,
            InsightType type
    ) {
        return "insights.rebuild|"
                + userId
                + "|"
                + start
                + "|"
                + end
                + "|"
                + g
                + "|"
                + type
                + "|"
                + RULE_VERSION;
    }

    private static LocalDate bucketEnd(
            LocalDate bucketStart,
            PeriodGranularity g,
            LocalDate hardEnd
    ) {
        LocalDate end = switch (g) {
            case DAY -> bucketStart;
            case WEEK -> bucketStart.plusDays(6);
            case MONTH -> bucketStart.withDayOfMonth(
                    bucketStart.lengthOfMonth()
            );
        };
        return end.isAfter(hardEnd) ? hardEnd : end;
    }

    private static LocalDate nextBucketStart(
            LocalDate bucketStart,
            PeriodGranularity g
    ) {
        return switch (g) {
            case DAY -> bucketStart.plusDays(1);
            case WEEK -> bucketStart.plusDays(7);
            case MONTH -> bucketStart.plusMonths(1)
                    .withDayOfMonth(1);
        };
    }

    // Lista as execuções do usuário para consultas internas.
    public List<RebuildRunResult> rawRuns(
            UUID userId,
            int page,
            int size
    ) {
        return rebuildRunPort
                .findByUserId(userId, page, size)
                .stream()
                .map(RebuildRunResult::from)
                .toList();
    }
}
