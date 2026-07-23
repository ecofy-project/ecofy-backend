package br.com.ecofy.ms_ingestion.core.port.in;

import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import br.com.ecofy.ms_ingestion.core.port.out.PageResult;

import java.util.UUID;

public interface ListImportJobsUseCase {

    record ListImportJobsQuery(
            UUID ownerId,
            ImportJobStatus status,
            int page,
            int size,
            String sortField,
            boolean ascending
    ) {
    }

    PageResult<ImportJob> list(ListImportJobsQuery query);
}
