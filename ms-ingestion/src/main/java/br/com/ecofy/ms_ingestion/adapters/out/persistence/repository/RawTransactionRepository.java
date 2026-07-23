package br.com.ecofy.ms_ingestion.adapters.out.persistence.repository;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.RawTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RawTransactionRepository extends JpaRepository<RawTransactionEntity, UUID> {

    @Query("""
            select t.rowHash
              from RawTransactionEntity t
             where t.importFile.id = :importFileId
               and t.rowHash in :rowHashes
            """)
    List<String> findExistingRowHashes(@Param("importFileId") UUID importFileId,
                                       @Param("rowHashes") Collection<String> rowHashes);

    void deleteByImportJobId(UUID importJobId);

    long countByImportJobId(UUID importJobId);
}
