package br.com.ecofy.ms_insights.core.port.out;

import br.com.ecofy.ms_insights.core.domain.rebuild.InsightRebuildRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RebuildRunPort {

    InsightRebuildRun save(InsightRebuildRun run);

    Optional<InsightRebuildRun> findById(UUID id);

    Optional<InsightRebuildRun> findActiveByIdempotencyKey(String idempotencyKey);

    List<InsightRebuildRun> findByUserId(UUID userId, int page, int size);

    long countByUserId(UUID userId);
}
