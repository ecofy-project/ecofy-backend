package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportErrorType;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import br.com.ecofy.ms_ingestion.core.port.in.StartImportJobUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportProcessingServiceTest {

    @Mock private SaveImportJobPort saveImportJobPort;
    @Mock private LoadImportJobPort loadImportJobPort;
    @Mock private SaveRawTransactionPort saveRawTransactionPort;
    @Mock private SaveImportErrorPort saveImportErrorPort;
    @Mock private SaveImportFilePort saveImportFilePort;
    @Mock private FileContentLoaderPort fileContentLoaderPort;
    @Mock private ParseCsvPort parseCsvPort;
    @Mock private ParseOfxPort parseOfxPort;
    @Mock private PublishTransactionForCategorizationPort publishTransactionForCategorizationPort;
    @Mock private PublishIngestionEventPort publishIngestionEventPort;

    private final UUID importFileId = UUID.randomUUID();

    private ImportProcessingService service(int maxErrors) {
        IngestionProperties props = new IngestionProperties();
        props.setMaxErrorsPerJob(maxErrors);
        return new ImportProcessingService(
                saveImportJobPort, loadImportJobPort, saveRawTransactionPort, saveImportErrorPort,
                saveImportFilePort, fileContentLoaderPort, parseCsvPort, parseOfxPort,
                publishTransactionForCategorizationPort, publishIngestionEventPort, props);
    }

    private void wireCommonMocks(ParseResult parseResult) {
        when(saveImportJobPort.save(any(ImportJob.class))).thenAnswer(inv -> inv.getArgument(0));
        ImportFile file = ImportFile.create("f.csv", "stored/path.csv", ImportFileType.CSV, 100L);
        when(saveImportFilePort.getById(importFileId)).thenReturn(file);
        when(fileContentLoaderPort.load("stored/path.csv")).thenReturn("ignored".getBytes());
        when(parseCsvPort.parse(any(ImportJob.class), anyString())).thenReturn(parseResult);
    }

    private RawTransaction tx() {
        return RawTransaction.create(UUID.randomUUID(), null, "desc",
                new TransactionDate(LocalDate.now()), new Money(BigDecimal.ONE, "BRL"), TransactionSourceType.FILE_CSV);
    }

    private ImportError err(int line) {
        return ImportError.create(UUID.randomUUID(), line, "raw", ImportErrorType.VALIDATION_ERROR, "bad line");
    }

    @Test
    void start_allValid_shouldCompleteWithFullCounters() {
        wireCommonMocks(ParseResult.of(List.of(tx(), tx()), List.of()));
        ImportProcessingService service = service(100);

        ImportJob job = service.start(new StartImportJobUseCase.StartImportJobCommand(importFileId));

        assertEquals(ImportJobStatus.COMPLETED, job.status());
        assertEquals(2, job.totalRecords());
        assertEquals(2, job.processedRecords());
        assertEquals(2, job.successCount());
        assertEquals(0, job.errorCount());
        verify(saveRawTransactionPort).saveAll(any());
        verify(publishTransactionForCategorizationPort).publish(any());
    }

    @Test
    void start_partialErrors_shouldCompleteWithErrors_andTrackCounts() {
        wireCommonMocks(ParseResult.of(List.of(tx(), tx()), List.of(err(3))));
        ImportProcessingService service = service(100);

        ImportJob job = service.start(new StartImportJobUseCase.StartImportJobCommand(importFileId));

        assertEquals(ImportJobStatus.COMPLETED_WITH_ERRORS, job.status());
        assertEquals(3, job.totalRecords());
        assertEquals(2, job.successCount());
        assertEquals(1, job.errorCount());
        verify(saveImportErrorPort).saveAll(any());
    }

    @Test
    void start_allErrorsNoValid_shouldFail() {
        wireCommonMocks(ParseResult.of(List.of(), List.of(err(2), err(3))));
        ImportProcessingService service = service(100);

        ImportJob job = service.start(new StartImportJobUseCase.StartImportJobCommand(importFileId));

        assertEquals(ImportJobStatus.FAILED, job.status());
        assertEquals(0, job.successCount());
        assertEquals(2, job.errorCount());
        verify(saveRawTransactionPort, never()).saveAll(any());
    }

    @Test
    void start_errorsAboveLimit_shouldFail_evenWithSomeValid() {
        wireCommonMocks(ParseResult.of(List.of(tx()), List.of(err(2))));
        ImportProcessingService service = service(0); // qualquer erro excede o limite

        ImportJob job = service.start(new StartImportJobUseCase.StartImportJobCommand(importFileId));

        assertEquals(ImportJobStatus.FAILED, job.status());
    }
}
