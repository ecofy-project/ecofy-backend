package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.*;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import br.com.ecofy.ms_ingestion.core.domain.event.ImportJobStatusChangedEvent;
import br.com.ecofy.ms_ingestion.core.domain.event.TransactionsImportedEvent;
import br.com.ecofy.ms_ingestion.core.port.in.RetryFailedImportsUseCase;
import br.com.ecofy.ms_ingestion.core.port.in.StartImportJobUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    private final IngestionProperties ingestionProperties;

    // Inicializa e valida todas as dependências do serviço de processamento de importação.
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
                                   IngestionProperties ingestionProperties) {

        this.saveImportJobPort = Objects.requireNonNull(saveImportJobPort, "saveImportJobPort must not be null");
        this.loadImportJobPort = Objects.requireNonNull(loadImportJobPort, "loadImportJobPort must not be null");
        this.saveRawTransactionPort = Objects.requireNonNull(saveRawTransactionPort, "saveRawTransactionPort must not be null");
        this.saveImportErrorPort = Objects.requireNonNull(saveImportErrorPort, "saveImportErrorPort must not be null");
        this.saveImportFilePort = Objects.requireNonNull(saveImportFilePort, "saveImportFilePort must not be null");
        this.fileContentLoaderPort = Objects.requireNonNull(fileContentLoaderPort, "fileContentLoaderPort must not be null");
        this.parseCsvPort = Objects.requireNonNull(parseCsvPort, "parseCsvPort must not be null");
        this.parseOfxPort = Objects.requireNonNull(parseOfxPort, "parseOfxPort must not be null");
        this.publishTransactionForCategorizationPort =
                Objects.requireNonNull(publishTransactionForCategorizationPort, "publishTransactionForCategorizationPort must not be null");
        this.publishIngestionEventPort =
                Objects.requireNonNull(publishIngestionEventPort, "publishIngestionEventPort must not be null");
        this.ingestionProperties = Objects.requireNonNull(ingestionProperties, "ingestionProperties must not be null");
    }

    // Cria um ImportJob para o arquivo informado e dispara o processamento completo do job.
    @Override
    public ImportJob start(StartImportJobCommand command) {
        if (command == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "command must not be null");
        }

        UUID importFileId = command.importFileId();
        if (importFileId == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "importFileId must not be null");
        }

        log.info("[ImportProcessingService] - [start] -> Iniciando job para importFileId={}", importFileId);

        ImportJob job;
        try {
            job = ImportJob.create(importFileId);
            job = saveImportJobPort.save(job);
        } catch (Exception e) {
            throw new PersistenceException("Failed to create/persist ImportJob", e);
        }

        processJob(job);

        return job;
    }

    // Processa um ImportJob: marca RUNNING, carrega arquivo, faz parse, persiste transações/erros e publica eventos/status.
    private void processJob(ImportJob job) {
        Objects.requireNonNull(job, "job must not be null");

        ImportJobStatus previousStatus = job.status();

        try {
            job.markRunning();
            saveImportJobPort.save(job);

            publishIngestionEventPort.publish(
                    new ImportJobStatusChangedEvent(
                            job.id(),
                            previousStatus,
                            job.status(),
                            job.updatedAt()
                    )
            );
        } catch (Exception e) {
            throw new PublishException("Failed to mark job RUNNING and publish status change", e);
        }

        List<RawTransaction> allTransactions = new ArrayList<>();
        List<ImportError> allErrors = new ArrayList<>();

        try {
            log.info("[ImportProcessingService] - [processJob] -> Processando job id={}", job.id());

            UUID importFileId = job.importFileId();

            ImportFile importFile;
            try {
                importFile = saveImportFilePort.getById(importFileId);
            } catch (Exception e) {
                throw new ImportFileNotFoundException(importFileId);
            }

            String storedPath = importFile.storedPath();
            if (storedPath == null || storedPath.isBlank()) {
                throw new ImportFileStoredPathMissingException(importFileId);
            }

            byte[] fileBytes;
            try {
                fileBytes = fileContentLoaderPort.load(storedPath);
            } catch (Exception e) {
                throw new StorageException("Failed to load stored file content", e);
            }

            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);

            List<RawTransaction> parsedTransactions;
            try {
                switch (importFile.type()) {
                    case CSV -> parsedTransactions = parseCsvPort.parse(job, fileContent);
                    case OFX -> parsedTransactions = parseOfxPort.parse(job, fileContent);
                    default -> throw new UnsupportedImportFileTypeException(String.valueOf(importFile.type()), importFileId);
                }
            } catch (IngestionException e) {
                throw e;
            } catch (Exception e) {
                throw new ParseException("Failed to parse import file", e);
            }

            allTransactions.addAll(parsedTransactions);
            // parsedErrors (quando existir) -> allErrors.addAll(parsedErrors);

            if (!allTransactions.isEmpty()) {
                try {
                    saveRawTransactionPort.saveAll(allTransactions);
                } catch (Exception e) {
                    throw new PersistenceException("Failed to persist raw transactions", e);
                }

                try {
                    publishTransactionForCategorizationPort.publish(allTransactions);
                    publishIngestionEventPort.publish(
                            new TransactionsImportedEvent(
                                    job.id(),
                                    allTransactions.stream().map(RawTransaction::id).toList()
                            )
                    );
                } catch (Exception e) {
                    throw new PublishException("Failed to publish transactions/events", e);
                }
            }

            if (!allErrors.isEmpty()) {
                try {
                    saveImportErrorPort.saveAll(allErrors);
                } catch (Exception e) {
                    throw new PersistenceException("Failed to persist import errors", e);
                }
            }

            if (allErrors.isEmpty()) {
                job.markCompleted();
            } else if (allErrors.size() > ingestionProperties.getMaxErrorsPerJob()) {
                job.markFailed();
            } else {
                job.markCompletedWithErrors();
            }

            saveImportJobPort.save(job);

            publishIngestionEventPort.publish(
                    new ImportJobStatusChangedEvent(
                            job.id(),
                            ImportJobStatus.RUNNING,
                            job.status(),
                            job.updatedAt()
                    )
            );

        } catch (IngestionException e) {
            failJob(job, e);
        } catch (Exception e) {
            failJob(job, new IngestionException(IngestionErrorCode.PERSISTENCE_ERROR, "Unexpected error while processing job", e));
        }
    }

    // Finaliza o job como FAILED, persistindo o status e publicando o evento de mudança de status com resiliência.
    private void failJob(ImportJob job, RuntimeException cause) {
        log.error(
                "[ImportProcessingService] - [processJob] -> Erro ao processar job id={} error={}",
                job.id(), cause.getMessage(), cause
        );

        try {
            job.markFailed();
            saveImportJobPort.save(job);

            publishIngestionEventPort.publish(
                    new ImportJobStatusChangedEvent(
                            job.id(),
                            ImportJobStatus.RUNNING,
                            job.status(),
                            job.updatedAt()
                    )
            );
        } catch (Exception e) {
            log.error(
                    "[ImportProcessingService] - [failJob] -> Falha ao marcar job como FAILED/publicar evento id={} error={}",
                    job.id(), e.getMessage(), e
            );
        }
    }

    // Reprocessa jobs elegíveis (FAILED/COMPLETED_WITH_ERRORS) respeitando o limite configurado no comando.
    @Override
    public void retry(RetryFailedImportsCommand command) {
        if (command == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "command must not be null");
        }

        int maxJobs = command.maxJobs();
        log.info("[ImportProcessingService] - [retry] -> Disparo de retry solicitado maxJobs={}", maxJobs);

        List<ImportJob> jobsToRetry;
        try {
            jobsToRetry = loadImportJobPort.loadJobsToRetry(maxJobs);
        } catch (Exception e) {
            throw new PersistenceException("Failed to load jobs eligible for retry", e);
        }

        if (jobsToRetry.isEmpty()) {
            log.info("[ImportProcessingService] - [retry] -> Nenhum job elegível para retry encontrado");
            return;
        }

        log.info("[ImportProcessingService] - [retry] -> Encontrados {} jobs para retry", jobsToRetry.size());

        for (ImportJob job : jobsToRetry) {
            ImportJobStatus status = job.status();
            if (status != ImportJobStatus.FAILED &&
                    status != ImportJobStatus.COMPLETED_WITH_ERRORS) {

                log.debug(
                        "[ImportProcessingService] - [retry] -> Ignorando job id={} com status={}",
                        job.id(), status
                );
                continue;
            }

            log.info(
                    "[ImportProcessingService] - [retry] -> Reprocessando job id={} statusAtual={}",
                    job.id(), status
            );
            processJob(job);
        }
    }

}
