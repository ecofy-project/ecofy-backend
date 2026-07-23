package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportAccessForbiddenException;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportFileStoredPathMissingException;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportProcessingTimeoutException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionErrorCode;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import br.com.ecofy.ms_ingestion.core.application.exception.PersistenceException;
import br.com.ecofy.ms_ingestion.core.application.exception.StorageException;
import br.com.ecofy.ms_ingestion.core.application.exception.UnsupportedImportFileTypeException;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import br.com.ecofy.ms_ingestion.core.domain.event.ImportJobStatusChangedEvent;
import br.com.ecofy.ms_ingestion.core.port.in.RetryFailedImportsUseCase;
import br.com.ecofy.ms_ingestion.core.port.in.StartImportJobUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.FileContentLoaderPort;
import br.com.ecofy.ms_ingestion.core.port.out.ImportRecordHandler;
import br.com.ecofy.ms_ingestion.core.port.out.LoadImportJobPort;
import br.com.ecofy.ms_ingestion.core.port.out.ParseCsvPort;
import br.com.ecofy.ms_ingestion.core.port.out.ParseOfxPort;
import br.com.ecofy.ms_ingestion.core.port.out.PublishIngestionEventPort;
import br.com.ecofy.ms_ingestion.core.port.out.PublishTransactionForCategorizationPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportErrorPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportFilePort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportJobPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveRawTransactionPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// Processa o job em streaming e lotes, com uma transação curta por lote e o job atualizado entre eles como checkpoint.
@Slf4j
@Service
public class ImportProcessingService implements StartImportJobUseCase, RetryFailedImportsUseCase {

    private final SaveImportJobPort saveImportJobPort;
    private final LoadImportJobPort loadImportJobPort;
    private final SaveRawTransactionPort saveRawTransactionPort;
    private final SaveImportErrorPort saveImportErrorPort;
    private final SaveImportFilePort saveImportFilePort;
    private final FileContentLoaderPort fileContentLoaderPort;
    private final ParseCsvPort parseCsvPort;
    private final ParseOfxPort parseOfxPort;
    private final PublishTransactionForCategorizationPort publishTransactionForCategorizationPort;
    private final PublishIngestionEventPort publishIngestionEventPort;
    private final IngestionProperties properties;
    private final MeterRegistry meterRegistry;

    public ImportProcessingService(SaveImportJobPort saveImportJobPort,
                                   LoadImportJobPort loadImportJobPort,
                                   SaveRawTransactionPort saveRawTransactionPort,
                                   SaveImportErrorPort saveImportErrorPort,
                                   SaveImportFilePort saveImportFilePort,
                                   FileContentLoaderPort fileContentLoaderPort,
                                   ParseCsvPort parseCsvPort,
                                   ParseOfxPort parseOfxPort,
                                   PublishTransactionForCategorizationPort publishTransactionForCategorizationPort,
                                   PublishIngestionEventPort publishIngestionEventPort,
                                   IngestionProperties properties,
                                   MeterRegistry meterRegistry) {

        this.saveImportJobPort = Objects.requireNonNull(saveImportJobPort, "saveImportJobPort must not be null");
        this.loadImportJobPort = Objects.requireNonNull(loadImportJobPort, "loadImportJobPort must not be null");
        this.saveRawTransactionPort =
                Objects.requireNonNull(saveRawTransactionPort, "saveRawTransactionPort must not be null");
        this.saveImportErrorPort = Objects.requireNonNull(saveImportErrorPort, "saveImportErrorPort must not be null");
        this.saveImportFilePort = Objects.requireNonNull(saveImportFilePort, "saveImportFilePort must not be null");
        this.fileContentLoaderPort =
                Objects.requireNonNull(fileContentLoaderPort, "fileContentLoaderPort must not be null");
        this.parseCsvPort = Objects.requireNonNull(parseCsvPort, "parseCsvPort must not be null");
        this.parseOfxPort = Objects.requireNonNull(parseOfxPort, "parseOfxPort must not be null");
        this.publishTransactionForCategorizationPort = Objects.requireNonNull(
                publishTransactionForCategorizationPort, "publishTransactionForCategorizationPort must not be null");
        this.publishIngestionEventPort =
                Objects.requireNonNull(publishIngestionEventPort, "publishIngestionEventPort must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    @Override
    public ImportJob start(StartImportJobCommand command) {
        if (command == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "command must not be null");
        }

        UUID importFileId = command.importFileId();
        if (importFileId == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "importFileId must not be null");
        }

        ImportFile file = saveImportFilePort.getById(importFileId);

        // Ownership checado mesmo aqui: o arquivo acabou de ser criado pelo mesmo request,
        // mas o use case é público e não deve confiar em quem o chama.
        if (!file.userId().equals(command.ownerId())) {
            throw new ImportAccessForbiddenException(importFileId);
        }

        log.info("[ImportProcessingService] - [start] -> Iniciando job para importFileId={}", importFileId);

        ImportJob job;
        try {
            job = ImportJob.create(importFileId, file.userId(), command.correlationId());
            job = saveImportJobPort.save(job);
        } catch (Exception e) {
            throw new PersistenceException("Failed to create/persist ImportJob", e);
        }

        meterRegistry.counter("ecofy.ingestion.job.total", "outcome", "created").increment();

        processJob(job, file);
        return job;
    }

    private void processJob(ImportJob job, ImportFile file) {
        Duration timeout = properties.getUpload().getProcessingTimeout();
        Timer.Sample sample = Timer.start(meterRegistry);

        job.markRunning(timeout);
        saveImportJobPort.save(job);
        publishStatusChange(job, ImportJobStatus.PENDING);

        String storedPath = file.storedPath();
        if (storedPath == null || storedPath.isBlank()) {
            failJob(job, new ImportFileStoredPathMissingException(file.id()), sample);
            throw new ImportFileStoredPathMissingException(file.id());
        }

        BatchAccumulator accumulator = new BatchAccumulator(job, file);

        try (InputStream in = fileContentLoaderPort.open(storedPath);
             Reader reader = strictReader(in)) {

            switch (file.type()) {
                case CSV -> parseCsvPort.parse(job, reader, accumulator);
                case OFX -> parseOfxPort.parse(job, reader, accumulator);
                default -> throw new UnsupportedImportFileTypeException(String.valueOf(file.type()), file.id());
            }

            // Último lote parcial.
            accumulator.flush();

            if (accumulator.timedOut) {
                ImportProcessingTimeoutException timeoutEx = new ImportProcessingTimeoutException(timeout);
                failJob(job, timeoutEx, sample);
                throw timeoutEx;
            }

            finishJob(job, sample);

        } catch (IngestionException e) {
            // Falha estrutural (§10.2). Os lotes já confirmados permanecem e os contadores
            // continuam verdadeiros: o job fica FAILED com published_records real, e não
            // com o zero enganoso que a versão anterior reportava.
            failJob(job, e, sample);
            throw e;
        } catch (IOException e) {
            StorageException wrapped = new StorageException("Error reading import file", e);
            failJob(job, wrapped, sample);
            throw wrapped;
        } catch (RuntimeException e) {
            IngestionException wrapped = new IngestionException(
                    IngestionErrorCode.INTERNAL_INGESTION_ERROR, "Unexpected error while processing job", e);
            failJob(job, wrapped, sample);
            throw wrapped;
        }
    }

    // Define o status final do job a partir do que foi efetivamente processado, tratando erro de linha como importação parcial.
    private void finishJob(ImportJob job, Timer.Sample sample) {
        if (job.errorCount() == 0) {
            job.markCompleted();
        } else if (job.errorCount() > properties.getMaxErrorsPerJob()) {
            // Excesso global de erros: o arquivo é considerado imprestável (§10.2).
            job.markFailed(IngestionErrorCode.INVALID_FILE_CONTENT.getCode(),
                    "Too many invalid rows: " + job.errorCount());
        } else {
            job.markCompletedWithErrors();
        }

        saveImportJobPort.save(job);
        publishStatusChange(job, ImportJobStatus.RUNNING);
        recordOutcome(job, sample);

        log.info("[ImportProcessingService] - [finishJob] -> Job finalizado id={} status={} total={} ok={} erros={} "
                        + "dup={} publicados={} errosTruncados={}",
                job.id(), job.status(), job.totalRecords(), job.successCount(), job.errorCount(),
                job.duplicateRecords(), job.publishedRecords(), job.errorsTruncated());
    }

    private void failJob(ImportJob job, IngestionException cause, Timer.Sample sample) {
        // A mensagem sanitizada vai para o banco; o stack trace só para o log (§12).
        log.error("[ImportProcessingService] - [failJob] -> Erro ao processar job id={} code={} detail={}",
                job.id(), cause.getErrorCode(), cause.getDetail(), cause);

        try {
            if (job.isTerminal()) {
                return;
            }
            job.markFailed(
                    cause.getErrorCode() != null ? cause.getErrorCode().getCode() : "INTERNAL_INGESTION_ERROR",
                    cause.getMessage());
            saveImportJobPort.save(job);
            publishStatusChange(job, ImportJobStatus.RUNNING);
            recordOutcome(job, sample);
        } catch (Exception e) {
            log.error("[ImportProcessingService] - [failJob] -> Falha ao marcar job FAILED id={} error={}",
                    job.id(), e.getMessage(), e);
        }
    }

    private void recordOutcome(ImportJob job, Timer.Sample sample) {
        sample.stop(meterRegistry.timer("ecofy.ingestion.processing.duration",
                "status", job.status().name()));
        meterRegistry.counter("ecofy.ingestion.job.total", "outcome", job.status().name()).increment();
    }

    private void publishStatusChange(ImportJob job, ImportJobStatus previous) {
        try {
            publishIngestionEventPort.publish(new ImportJobStatusChangedEvent(
                    job.id(), previous, job.status(), job.updatedAt()));
        } catch (Exception e) {
            // Evento de status é observabilidade: perdê-lo não pode derrubar a importação
            // nem reverter lotes já confirmados.
            log.warn("[ImportProcessingService] - [publishStatusChange] -> Falha ao publicar status jobId={} error={}",
                    job.id(), e.getMessage());
        }
    }

    // Cria um reader com decodificação estrita, para que bytes inválidos falhem em vez de virar texto corrompido.
    private Reader strictReader(InputStream in) {
        CharsetDecoder decoder = properties.getUpload().resolveCharset().newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        return new BufferedReader(new InputStreamReader(in, decoder), 8192);
    }

    // Acumula registros até fechar um lote, então persiste, publica e libera a memória.
    private final class BatchAccumulator implements ImportRecordHandler {

        private final ImportJob job;
        private final ImportFile file;
        private final int batchSize;
        private final int maxRecordedErrors;
        private final Set<String> seenRowHashes = new HashSet<>();

        private final List<RawTransaction> pendingTransactions;
        private final List<ImportError> pendingErrors = new ArrayList<>();

        private int batchValid;
        private int batchInvalid;
        private int batchDuplicated;
        private int batchRecordedErrors;

        private boolean timedOut;

        private BatchAccumulator(ImportJob job, ImportFile file) {
            this.job = job;
            this.file = file;
            this.batchSize = properties.getBatchSize();
            this.maxRecordedErrors = properties.getUpload().getMaxRecordedErrors();
            this.pendingTransactions = new ArrayList<>(batchSize);
        }

        @Override
        public void onValid(RawTransaction transaction) {
            // Dedupe intra-arquivo: a mesma linha repetida não vira duas transações. A
            // constraint no banco cobre o resto (retry, concorrência).
            if (!seenRowHashes.add(transaction.rowHash())) {
                batchDuplicated++;
                maybeFlush();
                return;
            }

            pendingTransactions.add(transaction);
            batchValid++;
            maybeFlush();
        }

        @Override
        public void onInvalid(ImportError error) {
            batchInvalid++;

            // Conta sempre; PERSISTE só até o teto (§10.4). Sem isso, um arquivo com
            // 100k linhas ruins geraria 100k inserts e um payload de erro inútil.
            if (job.recordedErrors() + batchRecordedErrors < maxRecordedErrors) {
                pendingErrors.add(error);
                batchRecordedErrors++;
            } else if (!job.errorsTruncated()) {
                job.markErrorsTruncated();
            }
            maybeFlush();
        }

        @Override
        public boolean continueProcessing() {
            if (timedOut) {
                return false;
            }
            if (job.isPastDeadline(Instant.now())) {
                // Ponto seguro: para entre registros, sem interromper thread nem transação.
                timedOut = true;
                log.warn("[ImportProcessingService] - [continueProcessing] -> Deadline excedido jobId={}", job.id());
                return false;
            }
            return true;
        }

        private void maybeFlush() {
            if (pendingTransactions.size() >= batchSize || pendingErrors.size() >= batchSize) {
                flush();
            }
        }

        private void flush() {
            if (pendingTransactions.isEmpty() && pendingErrors.isEmpty()
                    && batchValid == 0 && batchInvalid == 0 && batchDuplicated == 0) {
                return;
            }

            int published = 0;

            if (!pendingTransactions.isEmpty()) {
                // Transação curta por lote.
                List<RawTransaction> inserted = saveRawTransactionPort.saveBatch(file.id(), pendingTransactions);

                // O que a constraint recusou já existia: conta como duplicado, não como novo.
                int rejected = pendingTransactions.size() - inserted.size();
                if (rejected > 0) {
                    batchValid -= rejected;
                    batchDuplicated += rejected;
                }

                if (!inserted.isEmpty()) {
                    published = publishBatch(inserted);
                }
            }

            if (!pendingErrors.isEmpty()) {
                saveImportErrorPort.saveAll(pendingErrors);
            }

            job.addBatchResult(batchValid, batchInvalid, batchDuplicated, published, batchRecordedErrors);

            // Checkpoint: o progresso fica visível para quem consulta o status durante o
            // processamento, e sobrevive a uma falha no lote seguinte.
            saveImportJobPort.save(job);

            meterRegistry.counter("ecofy.ingestion.record.total", "outcome", "valid").increment(batchValid);
            meterRegistry.counter("ecofy.ingestion.record.total", "outcome", "invalid").increment(batchInvalid);
            meterRegistry.counter("ecofy.ingestion.record.total", "outcome", "duplicated").increment(batchDuplicated);

            pendingTransactions.clear();
            pendingErrors.clear();
            batchValid = 0;
            batchInvalid = 0;
            batchDuplicated = 0;
            batchRecordedErrors = 0;
        }

        // Publica o lote sem derrubar o job em falha, contando apenas o que o broker confirmou.
        private int publishBatch(List<RawTransaction> transactions) {
            try {
                int published = publishTransactionForCategorizationPort.publish(transactions, job.correlationId());
                meterRegistry.counter("ecofy.ingestion.kafka.publish.total", "outcome", "success")
                        .increment(published);
                return published;
            } catch (Exception e) {
                meterRegistry.counter("ecofy.ingestion.kafka.publish.total", "outcome", "failure")
                        .increment(transactions.size());
                log.error("[ImportProcessingService] - [publishBatch] -> Falha ao publicar lote jobId={} size={} error={}",
                        job.id(), transactions.size(), e.getMessage());
                return 0;
            }
        }
    }

    @Override
    public void retry(RetryFailedImportsCommand command) {
        if (command == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "command must not be null");
        }

        int maxJobs = command.maxJobs() > 0 ? command.maxJobs() : properties.getMaxJobsPerRetry();
        log.info("[ImportProcessingService] - [retry] -> Disparo de retry solicitado maxJobs={}", maxJobs);

        List<ImportJob> jobsToRetry;
        try {
            jobsToRetry = loadImportJobPort.loadJobsToRetry(maxJobs);
        } catch (Exception e) {
            throw new PersistenceException("Failed to load jobs eligible for retry", e);
        }

        for (ImportJob job : jobsToRetry) {
            if (!job.isTerminal()) {
                continue;
            }
            try {
                ImportFile file = saveImportFilePort.getById(job.importFileId());

                // Reabre explicitamente: sair de um estado final é operação nomeada, não
                // efeito colateral (§12). Os contadores zeram e são recontados do zero.
                job.reopenForRetry();
                saveImportJobPort.save(job);

                log.info("[ImportProcessingService] - [retry] -> Reprocessando job id={}", job.id());
                processJob(job, file);
            } catch (RuntimeException e) {
                // Um job problemático não pode impedir o retry dos demais.
                log.warn("[ImportProcessingService] - [retry] -> Falha ao reprocessar job id={} error={}",
                        job.id(), e.getMessage());
            }
        }
    }
}
