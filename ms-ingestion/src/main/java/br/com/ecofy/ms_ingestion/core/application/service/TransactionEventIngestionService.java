package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.core.application.exception.*;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

// Coordena a ingestão de transações recebidas por eventos.
@Slf4j
@Service
public class TransactionEventIngestionService implements IngestTransactionEventUseCase {

    private static final Duration EVENT_PROCESSING_TIMEOUT = Duration.ofMinutes(5);

    private final SaveRawTransactionPort saveRawTransactionPort;
    private final PublishTransactionForCategorizationPort publishTransactionForCategorizationPort;
    private final SaveImportFilePort saveImportFilePort;
    private final SaveImportJobPort saveImportJobPort;

    public TransactionEventIngestionService(
            SaveRawTransactionPort saveRawTransactionPort,
            PublishTransactionForCategorizationPort publishTransactionForCategorizationPort,
            SaveImportFilePort saveImportFilePort,
            SaveImportJobPort saveImportJobPort) {
        this.saveRawTransactionPort =
                Objects.requireNonNull(saveRawTransactionPort, "saveRawTransactionPort must not be null");
        this.publishTransactionForCategorizationPort = Objects.requireNonNull(
                publishTransactionForCategorizationPort, "publishTransactionForCategorizationPort must not be null");
        this.saveImportFilePort = Objects.requireNonNull(saveImportFilePort, "saveImportFilePort must not be null");
        this.saveImportJobPort = Objects.requireNonNull(saveImportJobPort, "saveImportJobPort must not be null");
    }

    // Processa o evento com idempotência, persistência e publicação para categorização.
    @Override
    public void ingest(IngestEventCommand command) {
        if (command == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "command must not be null");
        }

        UUID ownerId = Objects.requireNonNull(command.ownerId(), "ownerId must not be null");
        List<RawTransaction> incoming = Objects.requireNonNull(command.transactions(), "transactions must not be null");

        log.info("[TransactionEventIngestionService] - [ingest] -> Ingerindo evento sourceSystem={} eventId={} totalTx={}",
                command.sourceSystem(), command.eventId(), incoming.size());

        if (incoming.isEmpty()) {
            throw new EmptyTransactionsPayloadException(command.sourceSystem(), command.eventId());
        }

        ImportFile savedFile;
        try {
            savedFile = saveImportFilePort.save(ImportFile.create(
                    UUID.randomUUID(),
                    ownerId,
                    "kafka-event:" + safe(command.sourceSystem()),
                    "kafka-event",
                    ImportFileType.EVENT,
                    0L,
                    eventHash(command.eventId()),
                    null
            ));
        } catch (ImportAlreadyProcessedException e) {
            log.info("[TransactionEventIngestionService] - [ingest] -> Evento já ingerido, ignorando eventId={}",
                    command.eventId());
            return;
        }

        ImportJob job;
        try {
            ImportJob newJob = ImportJob.create(savedFile.id(), ownerId, command.correlationId());
            newJob.markRunning(EVENT_PROCESSING_TIMEOUT);
            job = saveImportJobPort.save(newJob);
        } catch (Exception e) {
            throw new PersistenceException("Failed to create synthetic ImportJob for event ingestion", e);
        }

        List<RawTransaction> transactions = new ArrayList<>(incoming.size());
        for (RawTransaction tx : incoming) {
            transactions.add(new RawTransaction(
                    tx.id(),
                    job.id(),
                    tx.externalId(),
                    tx.description(),
                    tx.date(),
                    tx.amount(),
                    tx.sourceType(),
                    tx.rowHash(),
                    tx.createdAt()
            ));
        }

        List<RawTransaction> inserted;
        try {
            inserted = saveRawTransactionPort.saveBatch(savedFile.id(), transactions);
        } catch (Exception e) {
            job.markFailed(IngestionErrorCode.PERSISTENCE_ERROR.getCode(), "Failed to persist transactions");
            trySave(job);
            throw new PersistenceException("Failed to persist raw transactions from event", e);
        }

        int published = 0;
        try {
            published = publishTransactionForCategorizationPort.publish(inserted, command.correlationId());
        } catch (Exception e) {
            log.error("[TransactionEventIngestionService] - [ingest] -> Falha ao publicar eventId={} error={}",
                    command.eventId(), e.getMessage());
        }

        int duplicated = transactions.size() - inserted.size();
        job.addBatchResult(inserted.size(), 0, duplicated, published, 0);
        job.markCompleted();
        trySave(job);
    }

    // Registra o estado do job sem interromper o fluxo em caso de falha.
    private void trySave(ImportJob job) {
        try {
            saveImportJobPort.save(job);
        } catch (Exception e) {
            log.warn("[TransactionEventIngestionService] - [trySave] -> Falha ao persistir status do job sintético id={} err={}",
                    job.id(), e.getMessage());
        }
    }

    // Gera uma chave de idempotência específica para o evento.
    private static String eventHash(String eventId) {
        String id = safe(eventId);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "event:" + HexFormat.of().formatHex(digest.digest(id.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }
}
