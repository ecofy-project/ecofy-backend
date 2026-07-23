package br.com.ecofy.ms_insights.adapters.out.persistence;

import br.com.ecofy.ms_insights.adapters.out.persistence.entity.InsightRebuildRunEntity;
import br.com.ecofy.ms_insights.adapters.out.persistence.repository.InsightRebuildRunRepository;
import br.com.ecofy.ms_insights.core.domain.rebuild.InsightRebuildRun;
import br.com.ecofy.ms_insights.core.domain.rebuild.RebuildStatus;
import br.com.ecofy.ms_insights.core.port.out.RebuildRunPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Persiste e consulta execuções de reconstrução de insights.
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightRebuildRunJpaAdapter implements RebuildRunPort {

    private static final List<RebuildStatus> ACTIVE = List.of(
            RebuildStatus.PENDING,
            RebuildStatus.RUNNING
    );

    private final InsightRebuildRunRepository repository;

    @Override
    @Transactional
    public InsightRebuildRun save(InsightRebuildRun run) {
        Objects.requireNonNull(run, "run must not be null");
        return toDomain(repository.save(toEntity(run)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsightRebuildRun> findById(UUID id) {
        return repository.findById(id)
                .map(InsightRebuildRunJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsightRebuildRun> findActiveByIdempotencyKey(
            String idempotencyKey
    ) {
        return repository
                .findFirstByIdempotencyKeyAndStatusInOrderByCreatedAtDesc(
                        idempotencyKey,
                        ACTIVE
                )
                .map(InsightRebuildRunJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsightRebuildRun> findByUserId(
            UUID userId,
            int page,
            int size
    ) {
        return repository.findByUserIdOrderByCreatedAtDesc(
                        userId,
                        PageRequest.of(page, size)
                )
                .map(InsightRebuildRunJpaAdapter::toDomain)
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserId(UUID userId) {
        return repository.countByUserId(userId);
    }

    private static InsightRebuildRunEntity toEntity(InsightRebuildRun d) {
        return InsightRebuildRunEntity.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .insightType(d.getInsightType())
                .periodStart(d.getPeriodStart())
                .periodEnd(d.getPeriodEnd())
                .granularity(d.getGranularity())
                .mode(d.getMode())
                .idempotencyKey(d.getIdempotencyKey())
                .correlationId(d.getCorrelationId())
                .status(d.getStatus())
                .checkpoint(d.getCheckpoint())
                .processedItems(d.getProcessedItems())
                .generatedInsights(d.getGeneratedInsights())
                .failedItems(d.getFailedItems())
                .lastErrorCode(d.getLastErrorCode())
                .createdAt(d.getCreatedAt())
                .startedAt(d.getStartedAt())
                .finishedAt(d.getFinishedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private static InsightRebuildRun toDomain(InsightRebuildRunEntity e) {
        return new InsightRebuildRun(
                e.getId(),
                e.getUserId(),
                e.getInsightType(),
                e.getPeriodStart(),
                e.getPeriodEnd(),
                e.getGranularity(),
                e.getMode(),
                e.getIdempotencyKey(),
                e.getCorrelationId(),
                e.getStatus(),
                e.getCheckpoint(),
                e.getProcessedItems(),
                e.getGeneratedInsights(),
                e.getFailedItems(),
                e.getLastErrorCode(),
                e.getCreatedAt(),
                e.getStartedAt(),
                e.getFinishedAt(),
                e.getUpdatedAt()
        );
    }
}
