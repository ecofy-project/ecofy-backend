package br.com.ecofy.ms_budgeting.adapters.in.sched;

import br.com.ecofy.ms_budgeting.core.application.command.CleanupBudgetsCommand;
import br.com.ecofy.ms_budgeting.core.port.in.CleanupBudgetsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetCleanupScheduler {

    private final CleanupBudgetsUseCase useCase;
    private final BudgetingSchedulingProperties props;
    private final Clock clock;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${ecofy.budgeting.scheduling.cleanup-cron:0 0 3 * * *}")
    public void cleanup() {
        if (!props.isCleanupEnabled()) {
            log.debug("[BudgetCleanupScheduler] SKIP cleanupEnabled=false");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("[BudgetCleanupScheduler] SKIP already running");
            return;
        }

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now(clock);

        LocalDate referenceDate = LocalDate.now(clock);
        int retentionDays = props.getCleanupRetentionDays();

        log.info("[BudgetCleanupScheduler] START runId={} referenceDate={} retentionDays={}",
                runId, referenceDate, retentionDays);

        try {
            var result = useCase.cleanup(new CleanupBudgetsCommand(runId, referenceDate, retentionDays));
            long tookMs = Duration.between(start, Instant.now(clock)).toMillis();

            log.info("[BudgetCleanupScheduler] DONE runId={} tookMs={} budgetsDeleted={} consumptionsDeleted={}",
                    runId, tookMs, result.budgetsDeleted(), result.consumptionsDeleted());
        } catch (Exception ex) {
            long tookMs = Duration.between(start, Instant.now(clock)).toMillis();
            log.error("[BudgetCleanupScheduler] FAIL runId={} tookMs={} error={}",
                    runId, tookMs, ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }

}
