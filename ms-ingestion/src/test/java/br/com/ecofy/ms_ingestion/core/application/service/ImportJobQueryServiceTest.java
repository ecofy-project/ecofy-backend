package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.core.application.exception.ImportJobNotFoundException;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import br.com.ecofy.ms_ingestion.core.port.in.GetImportJobStatusUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.LoadImportJobPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobQueryServiceTest {

    @Mock
    private LoadImportJobPort loadImportJobPort;

    @Test
    void getById_shouldReturnJobWithUpdatedCounters() {
        UUID jobId = UUID.randomUUID();
        ImportJob job = new ImportJob(jobId, UUID.randomUUID(), ImportJobStatus.COMPLETED_WITH_ERRORS,
                10, 10, 8, 2, Instant.now(), Instant.now(), Instant.now(), Instant.now());
        when(loadImportJobPort.loadById(jobId)).thenReturn(Optional.of(job));

        var service = new ImportJobQueryService(loadImportJobPort);
        GetImportJobStatusUseCase.ImportJobStatusView view = service.getById(jobId);

        assertEquals(ImportJobStatus.COMPLETED_WITH_ERRORS, view.job().status());
        assertEquals(10, view.job().totalRecords());
        assertEquals(8, view.job().successCount());
        assertEquals(2, view.job().errorCount());
    }

    @Test
    void getById_shouldThrowNotFound_whenAbsent() {
        UUID jobId = UUID.randomUUID();
        when(loadImportJobPort.loadById(jobId)).thenReturn(Optional.empty());

        var service = new ImportJobQueryService(loadImportJobPort);
        assertThrows(ImportJobNotFoundException.class, () -> service.getById(jobId));
    }
}
