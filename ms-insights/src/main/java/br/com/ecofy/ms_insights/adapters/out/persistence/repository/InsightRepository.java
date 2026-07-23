package br.com.ecofy.ms_insights.adapters.out.persistence.repository;

import br.com.ecofy.ms_insights.adapters.out.persistence.entity.InsightEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsightRepository extends JpaRepository<InsightEntity, UUID> {
    List<InsightEntity> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
    List<InsightEntity> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<InsightEntity> findByUserId(UUID userId, Pageable pageable);
}
