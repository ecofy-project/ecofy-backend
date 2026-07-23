package br.com.ecofy.ms_insights.core.port.out;

import br.com.ecofy.ms_insights.core.domain.outbox.OutboxEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface OutboxEventPort {

    void save(OutboxEvent event);

    List<OutboxEvent> claimPendingBatch(int batchSize, Instant now);

    void markPublished(OutboxEvent event, Instant publishedAt);

    void markFailed(OutboxEvent event, String errorCode, Instant nextAttemptAt, Instant now);

    void markDiscarded(OutboxEvent event, String errorCode, Instant now);

    int releaseStuck(Duration timeout, Instant now);

    int deletePublishedOlderThan(Duration retention, Instant now, int maxRows);

    Instant oldestPendingCreatedAt();

    long countStuckProcessing(Duration timeout, Instant now);

    long countDiscarded();
}
