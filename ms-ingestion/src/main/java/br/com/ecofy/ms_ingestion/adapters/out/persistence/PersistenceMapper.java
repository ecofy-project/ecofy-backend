package br.com.ecofy.ms_ingestion.adapters.out.persistence;

import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportErrorEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportFileEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.ImportJobEntity;
import br.com.ecofy.ms_ingestion.adapters.out.persistence.entity.RawTransactionEntity;
import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;

import java.time.Instant;
import java.util.Objects;

// Centraliza a conversão entre o domínio e as entidades persistidas.
final class PersistenceMapper {

    private PersistenceMapper() {
    }

    // Converte a transação bruta em entidade e associa seus vínculos.
    static RawTransactionEntity toEntity(
            RawTransaction tx,
            ImportJobEntity jobEntity,
            ImportFileEntity fileEntity
    ) {
        Objects.requireNonNull(tx, "tx must not be null");
        Objects.requireNonNull(jobEntity, "jobEntity must not be null");
        Objects.requireNonNull(fileEntity, "fileEntity must not be null");

        RawTransactionEntity e = new RawTransactionEntity();
        e.setId(tx.id());
        e.setImportJob(jobEntity);
        e.setImportFile(fileEntity);

        e.setTransactionDate(tx.date().value());
        e.setDescription(tx.description());

        e.setAmount(tx.amount().amount());
        e.setCurrency(tx.amount().currency());

        e.setSourceType(tx.sourceType());
        e.setExternalId(tx.externalId());

        e.setRawPayload(null);

        e.setRowHash(tx.rowHash());
        e.setCreatedAt(tx.createdAt());
        return e;
    }

    // Converte a entidade persistida em uma transação bruta.
    static RawTransaction toDomain(RawTransactionEntity e) {
        Objects.requireNonNull(e, "entity must not be null");
        Objects.requireNonNull(e.getImportJob(), "importJob must not be null");

        Money money = new Money(e.getAmount(), e.getCurrency());
        TransactionDate date = new TransactionDate(e.getTransactionDate());

        return new RawTransaction(
                e.getId(),
                e.getImportJob().getId(),
                e.getExternalId(),
                e.getDescription(),
                date,
                money,
                e.getSourceType(),
                e.getRowHash(),
                e.getCreatedAt()
        );
    }

    // Converte o arquivo importado em uma entidade persistível.
    static ImportFileEntity toEntity(ImportFile file) {
        Objects.requireNonNull(file, "file must not be null");

        ImportFileEntity e = new ImportFileEntity();
        e.setId(file.id());
        e.setUserId(file.userId());

        e.setOriginalFilename(file.originalFileName());
        e.setStoredFilename(file.storedPath());
        e.setFileType(file.type());

        e.setSourceType(resolveSourceType(file.type()));

        e.setContentType(null);

        e.setFileHash(file.fileHash());
        e.setIdempotencyKey(file.idempotencyKey());

        e.setSizeBytes(file.sizeBytes());
        e.setUploadedAt(file.uploadedAt());

        Instant created = file.uploadedAt() != null
                ? file.uploadedAt()
                : Instant.now();

        e.setCreatedAt(created);

        return e;
    }

    // Converte a entidade persistida em um arquivo importado.
    static ImportFile toDomain(ImportFileEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return new ImportFile(
                e.getId(),
                e.getUserId(),
                e.getOriginalFilename(),
                e.getStoredFilename(),
                e.getFileType(),
                e.getSizeBytes(),
                e.getFileHash(),
                e.getIdempotencyKey(),
                e.getUploadedAt()
        );
    }

    // Resolve a origem da transação pelo tipo do arquivo.
    private static TransactionSourceType resolveSourceType(ImportFileType type) {
        if (type == null) {
            return TransactionSourceType.MANUAL_ENTRY;
        }

        return switch (type) {
            case CSV -> TransactionSourceType.FILE_CSV;
            case OFX -> TransactionSourceType.FILE_OFX;
            case EVENT -> TransactionSourceType.KAFKA_EVENT;
        };
    }

    // Converte o job de importação em entidade e associa seu arquivo.
    static ImportJobEntity toEntity(ImportJob job, ImportFileEntity fileEntity) {
        Objects.requireNonNull(job, "job must not be null");
        Objects.requireNonNull(fileEntity, "fileEntity must not be null");

        ImportJobEntity e = new ImportJobEntity();
        e.setId(job.id());
        e.setImportFile(fileEntity);
        e.setUserId(job.userId());
        e.setStatus(job.status());
        e.setTotalRecords(job.totalRecords());
        e.setProcessedRecords(job.processedRecords());
        e.setSuccessCount(job.successCount());
        e.setErrorCount(job.errorCount());
        e.setDuplicateRecords(job.duplicateRecords());
        e.setPublishedRecords(job.publishedRecords());
        e.setRecordedErrors(job.recordedErrors());
        e.setErrorsTruncated(job.errorsTruncated());
        e.setFailureCode(job.failureCode());
        e.setFailureReason(job.failureReason());
        e.setDeadlineAt(job.deadlineAt());
        e.setCorrelationId(job.correlationId());
        e.setStartedAt(job.startedAt());
        e.setFinishedAt(job.finishedAt());
        e.setCreatedAt(job.createdAt());
        e.setUpdatedAt(job.updatedAt());
        return e;
    }

    // Converte a entidade persistida em um job de importação.
    static ImportJob toDomain(ImportJobEntity e) {
        Objects.requireNonNull(e, "entity must not be null");
        ImportFileEntity file = Objects.requireNonNull(
                e.getImportFile(), "importFile must not be null"
        );

        ImportJobStatus status = e.getStatus();

        return new ImportJob(
                e.getId(),
                file.getId(),
                e.getUserId(),
                status,
                e.getTotalRecords(),
                e.getProcessedRecords(),
                e.getSuccessCount(),
                e.getErrorCount(),
                e.getDuplicateRecords(),
                e.getPublishedRecords(),
                e.getRecordedErrors(),
                e.isErrorsTruncated(),
                e.getFailureCode(),
                e.getFailureReason(),
                e.getDeadlineAt(),
                e.getCorrelationId(),
                e.getStartedAt(),
                e.getFinishedAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // Converte o erro de importação em entidade e associa seu job.
    static ImportErrorEntity toEntity(ImportError error, ImportJobEntity jobEntity) {
        Objects.requireNonNull(error, "error must not be null");
        Objects.requireNonNull(jobEntity, "jobEntity must not be null");

        ImportErrorEntity e = new ImportErrorEntity();
        e.setId(error.id());
        e.setImportJob(jobEntity);
        e.setLineNumber(error.lineNumber());
        e.setRawLine(error.rawContent());
        e.setMessage(error.errorMessage());
        e.setErrorType(error.errorType());
        e.setCreatedAt(error.createdAt());
        return e;
    }

    // Converte a entidade persistida em um erro de importação.
    static ImportError toDomain(ImportErrorEntity e) {
        Objects.requireNonNull(e, "entity must not be null");
        Objects.requireNonNull(e.getImportJob(), "importJob must not be null");

        return new ImportError(
                e.getId(),
                e.getImportJob().getId(),
                e.getLineNumber(),
                e.getRawLine(),
                e.getErrorType(),
                e.getMessage(),
                e.getCreatedAt()
        );
    }
}
