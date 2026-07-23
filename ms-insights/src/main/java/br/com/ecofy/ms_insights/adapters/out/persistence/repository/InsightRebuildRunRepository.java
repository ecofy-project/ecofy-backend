package br.com.ecofy.ms_insights.adapters.out.persistence.repository;

import br.com.ecofy.ms_insights.adapters.out.persistence.entity.InsightRebuildRunEntity;
import br.com.ecofy.ms_insights.core.domain.rebuild.RebuildStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsightRebuildRunRepository extends JpaRepository<InsightRebuildRunEntity, UUID> {

    Optional<InsightRebuildRunEntity> findFirstByIdempotencyKeyAndStatusInOrderByCreatedAtDesc(
            String idempotencyKey, List<RebuildStatus> statuses);

    Page<InsightRebuildRunEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);
}
