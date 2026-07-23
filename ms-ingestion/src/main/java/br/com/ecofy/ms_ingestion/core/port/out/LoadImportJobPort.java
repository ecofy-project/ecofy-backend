package br.com.ecofy.ms_ingestion.core.port.out;

import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadImportJobPort {

    Optional<ImportJob> loadById(UUID id);

    Optional<ImportJob> findByImportFileId(UUID importFileId);

    List<ImportJob> loadJobsToRetry(int maxJobs);

    PageResult<ImportJob> findByOwner(UUID ownerId,
                                      ImportJobStatus statusFilter,
                                      int page,
                                      int size,
                                      String sortField,
                                      boolean ascending);
}
