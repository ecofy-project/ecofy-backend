package br.com.ecofy.ms_budgeting.adapters.out.health;

import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.port.out.OutboxEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Component("budgetingOutbox")
// Reporta a integridade operacional da entrega de eventos pela Outbox.
public class OutboxHealthIndicator implements HealthIndicator {

    private final OutboxEventPort outboxPort;
    private final Duration staleThreshold;
    private final Duration processingTimeout;

    public OutboxHealthIndicator(OutboxEventPort outboxPort, BudgetingProperties props) {
        this.outboxPort = Objects.requireNonNull(outboxPort, "outboxPort must not be null");
        this.processingTimeout = props.outbox().processingTimeout();
        this.staleThreshold = processingTimeout.multipliedBy(3);
    }

    @Override
    // Avalia a integridade da Outbox com base no backlog e nos eventos degradados.
    public Health health() {
        Instant now = Instant.now();

        try {
            Instant oldestPending = outboxPort.oldestPendingCreatedAt();
            long stuck = outboxPort.countStuckProcessing(processingTimeout, now);
            long discarded = outboxPort.countDiscarded();

            long oldestAgeSeconds = oldestPending == null
                    ? 0
                    : Duration.between(oldestPending, now).getSeconds();

            boolean stale = oldestPending != null
                    && oldestAgeSeconds > staleThreshold.getSeconds();
            boolean degraded = stuck > 0 || discarded > 0;

            Health.Builder builder = stale ? Health.down() : Health.up();

            builder.withDetail("oldestPendingAgeSeconds", oldestAgeSeconds)
                    .withDetail("staleThresholdSeconds", staleThreshold.getSeconds())
                    .withDetail("stuckProcessing", stuck)
                    .withDetail("discarded", discarded)
                    .withDetail("degraded", degraded);

            if (stale) {
                builder.withDetail("reason", "outbox backlog is stale; publisher not draining");
            }

            return builder.build();
        } catch (Exception e) {
            log.warn(
                    "[OutboxHealthIndicator] - [health] -> falha ao inspecionar outbox error={}",
                    e.getMessage()
            );

            return Health.unknown()
                    .withDetail("error", e.getClass().getSimpleName())
                    .build();
        }
    }
}
