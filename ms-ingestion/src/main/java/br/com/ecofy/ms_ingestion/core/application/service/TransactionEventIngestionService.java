package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.core.application.exception.EmptyTransactionsPayloadException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionErrorCode;
import br.com.ecofy.ms_ingestion.core.application.exception.PersistenceException;
import br.com.ecofy.ms_ingestion.core.application.exception.PublishException;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;
import br.com.ecofy.ms_ingestion.core.port.in.IngestTransactionEventUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.PublishTransactionForCategorizationPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportFilePort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportJobPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveRawTransactionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class TransactionEventIngestionService implements IngestTransactionEventUseCase {

    private final SaveRawTransactionPort saveRawTransactionPort;
    private final PublishTransactionForCategorizationPort publishTransactionForCategorizationPort;
    private final SaveImportFilePort saveImportFilePort;
    private final SaveImportJobPort saveImportJobPort;

    // Inicializa e valida as dependências do serviço de ingestão via eventos.
    public TransactionEventIngestionService(SaveRawTransactionPort saveRawTransactionPort,
                                            PublishTransactionForCategorizationPort publishTransactionForCategorizationPort,
                                            SaveImportFilePort saveImportFilePort,
                                            SaveImportJobPort saveImportJobPort) {
        this.saveRawTransactionPort = Objects.requireNonNull(saveRawTransactionPort, "saveRawTransactionPort must not be null");
        this.publishTransactionForCategorizationPort =
                Objects.requireNonNull(publishTransactionForCategorizationPort, "publishTransactionForCategorizationPort must not be null");
        this.saveImportFilePort = Objects.requireNonNull(saveImportFilePort, "saveImportFilePort must not be null");
        this.saveImportJobPort = Objects.requireNonNull(saveImportJobPort, "saveImportJobPort must not be null");
    }

    /**
     * Persiste transações vindas de evento Kafka e publica para categorização.
     *
     * Correção Dia 4: a persistência de RawTransaction exige um ImportJob/ImportFile
     * válidos (FK no schema). Antes, o mapper gerava um importJobId ALEATÓRIO inexistente,
     * fazendo o consumer falhar sistematicamente. Agora criamos um ImportFile + ImportJob
     * SINTÉTICOS (origem EVENT) para o lote e vinculamos as transações a esse job.
     */
    @Override
    public void ingest(IngestEventCommand command) {
        if (command == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "command must not be null");
        }

        List<RawTransaction> incoming = Objects.requireNonNull(
                command.transactions(),
                "transactions must not be null"
        );

        log.info(
                "[TransactionEventIngestionService] - [ingest] -> Ingerindo evento sourceSystem={} payloadId={} totalTx={}",
                command.sourceSystem(), command.payloadId(), incoming.size()
        );

        if (incoming.isEmpty()) {
            throw new EmptyTransactionsPayloadException(command.sourceSystem(), command.payloadId());
        }

        // 1) ImportFile + ImportJob sintéticos para o lote do evento (satisfaz as FKs).
        final ImportJob job;
        try {
            ImportFile syntheticFile = ImportFile.create(
                    "kafka-event:" + safe(command.sourceSystem()),
                    "kafka-event",
                    ImportFileType.EVENT,
                    0L
            );
            ImportFile savedFile = saveImportFilePort.save(syntheticFile);

            ImportJob newJob = ImportJob.create(savedFile.id());
            newJob.markRunning();
            job = saveImportJobPort.save(newJob);
        } catch (Exception e) {
            throw new PersistenceException("Failed to create synthetic ImportFile/ImportJob for event ingestion", e);
        }

        // 2) Reassocia as transações ao job sintético (o importJobId anterior não existia).
        List<RawTransaction> transactions = new ArrayList<>(incoming.size());
        for (RawTransaction tx : incoming) {
            transactions.add(RawTransaction.create(
                    job.id(),
                    tx.externalId(),
                    tx.description(),
                    tx.date(),
                    tx.amount(),
                    tx.sourceType()
            ));
        }

        // 3) Persiste + publica.
        try {
            saveRawTransactionPort.saveAll(transactions);
        } catch (Exception e) {
            job.markFailed();
            trySave(job);
            throw new PersistenceException("Failed to persist raw transactions from event", e);
        }

        try {
            publishTransactionForCategorizationPort.publish(transactions);
        } catch (Exception e) {
            job.markFailed();
            trySave(job);
            throw new PublishException("Failed to publish transactions for categorization", e);
        }

        // 4) Contadores + status final do job sintético.
        int count = transactions.size();
        job.updateCounts(count, count, count, 0);
        job.markCompleted();
        trySave(job);
    }

    private void trySave(ImportJob job) {
        try {
            saveImportJobPort.save(job);
        } catch (Exception e) {
            log.warn("[TransactionEventIngestionService] - [ingest] -> Falha ao persistir status do job sintético id={} err={}",
                    job.id(), e.getMessage());
        }
    }

    private static String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }
}
