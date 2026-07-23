package br.com.ecofy.ms_ingestion.adapters.out.persistence.repository;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportErrorEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImportErrorRepository extends JpaRepository<ImportErrorEntity, UUID> {

    List<ImportErrorEntity> findByImportJobIdOrderByLineNumberAsc(UUID importJobId, Pageable pageable);

    void deleteByImportJobId(UUID importJobId);
}
