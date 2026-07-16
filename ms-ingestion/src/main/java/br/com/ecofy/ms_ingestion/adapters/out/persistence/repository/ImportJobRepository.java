package br.com.ecofy.ms_ingestion.adapters.out.persistence.repository;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportJobEntity;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, UUID> {

    // Query paginada por status (mais antigos primeiro) — evita carregar toda a tabela em memória no retry.
    List<ImportJobEntity> findByStatusInOrderByUpdatedAtAsc(Collection<ImportJobStatus> statuses, Pageable pageable);
}