package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.core.application.exception.EmptyTransactionsPayloadException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import br.com.ecofy.ms_ingestion.core.port.in.IngestTransactionEventUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.PublishTransactionForCategorizationPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportFilePort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportJobPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveRawTransactionPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventIngestionServiceTest {

    @Mock private SaveRawTransactionPort saveRawTransactionPort;
    @Mock private PublishTransactionForCategorizationPort publishTransactionForCategorizationPort;
    @Mock private SaveImportFilePort saveImportFilePort;
    @Mock private SaveImportJobPort saveImportJobPort;

    private TransactionEventIngestionService service() {
        return new TransactionEventIngestionService(
                saveRawTransactionPort, publishTransactionForCategorizationPort,
                saveImportFilePort, saveImportJobPort);
    }

    private RawTransaction incomingTx() {
        // Simula o cenário do bug: importJobId aleatório inexistente.
        return RawTransaction.create(UUID.randomUUID(), "ext-1", "Event tx",
                new TransactionDate(LocalDate.now()), new Money(BigDecimal.TEN, "BRL"), TransactionSourceType.KAFKA_EVENT);
    }

    @Test
    void ingest_shouldCreateSyntheticJob_andPersist_withoutFailingOnMissingImportJob() {
        when(saveImportFilePort.save(any(ImportFile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(saveImportJobPort.save(any(ImportJob.class))).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new IngestTransactionEventUseCase.IngestEventCommand("bank-x", "payload-1", List.of(incomingTx()));

        assertDoesNotThrow(() -> service().ingest(cmd));

        // Cria ImportFile + ImportJob sintéticos ANTES de persistir (satisfaz as FKs).
        verify(saveImportFilePort).save(any(ImportFile.class));

        ArgumentCaptor<ImportJob> jobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        verify(saveImportJobPort, atLeastOnce()).save(jobCaptor.capture());
        UUID syntheticJobId = jobCaptor.getAllValues().get(0).id();

        // As transações são reassociadas ao job sintético (não ao importJobId aleatório original).
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RawTransaction>> txCaptor = ArgumentCaptor.forClass(List.class);
        verify(saveRawTransactionPort).saveAll(txCaptor.capture());
        assertFalse(txCaptor.getValue().isEmpty());
        assertTrue(txCaptor.getValue().stream().allMatch(t -> t.importJobId().equals(syntheticJobId)));

        verify(publishTransactionForCategorizationPort).publish(any());
    }

    @Test
    void ingest_emptyTransactions_shouldThrowControlledError() {
        var cmd = new IngestTransactionEventUseCase.IngestEventCommand("bank-x", "payload-1", List.of());

        assertThrows(EmptyTransactionsPayloadException.class, () -> service().ingest(cmd));
        verifyNoInteractions(saveRawTransactionPort, publishTransactionForCategorizationPort);
    }

    @Test
    void ingest_nullCommand_shouldThrowIngestionException() {
        assertThrows(IngestionException.class, () -> service().ingest(null));
    }
}
