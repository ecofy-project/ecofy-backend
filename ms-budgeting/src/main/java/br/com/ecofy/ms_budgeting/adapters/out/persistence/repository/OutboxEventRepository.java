package br.com.ecofy.ms_budgeting.adapters.out.persistence.repository;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.OutboxEventEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    // Seleciona o lote elegível pulando registros já reservados, evitando disputa entre publishers.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select e
              from OutboxEventEntity e
             where e.status = br.com.ecofy.ms_budgeting.core.domain.outbox.OutboxStatus.PENDING
                or (e.status = br.com.ecofy.ms_budgeting.core.domain.outbox.OutboxStatus.FAILED
                    and e.nextAttemptAt <= :now)
             order by e.createdAt
             limit :batchSize
            """)
    List<OutboxEventEntity> lockEligibleBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Query("""
            select e
              from OutboxEventEntity e
             where e.status = br.com.ecofy.ms_budgeting.core.domain.outbox.OutboxStatus.PROCESSING
               and e.processingStartedAt < :threshold
            """)
    List<OutboxEventEntity> findStuckProcessing(@Param("threshold") Instant threshold);

    @Modifying
    @Query(value = """
            DELETE FROM outbox_events
             WHERE id IN (
                   SELECT id FROM outbox_events
                    WHERE status = 'PUBLISHED'
                      AND published_at < :threshold
                    ORDER BY published_at
                    LIMIT :maxRows)
            """, nativeQuery = true)
    int deletePublishedBatch(@Param("threshold") Instant threshold, @Param("maxRows") int maxRows);

    @Query(value = """
            SELECT MIN(created_at) FROM outbox_events
             WHERE status IN ('PENDING', 'FAILED', 'PROCESSING')
            """, nativeQuery = true)
    Instant findOldestUnpublishedCreatedAt();

    @Query("""
            select count(e)
              from OutboxEventEntity e
             where e.status = br.com.ecofy.ms_budgeting.core.domain.outbox.OutboxStatus.PROCESSING
               and e.processingStartedAt < :threshold
            """)
    long countStuckProcessing(@Param("threshold") Instant threshold);

    @Query("""
            select count(e)
              from OutboxEventEntity e
             where e.status = br.com.ecofy.ms_budgeting.core.domain.outbox.OutboxStatus.DISCARDED
            """)
    long countDiscarded();
}
