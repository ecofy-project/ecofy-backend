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

final class PersistenceMapper {

    private PersistenceMapper() {
    }

    // RAW TRANSACTION

    // Mapeia RawTransaction (domínio) para RawTransactionEntity vinculando ImportJob/ImportFile e campos persistidos.
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

        // domínio ainda não expõe rawPayload
        e.setRawPayload(null);

        e.setCreatedAt(tx.createdAt());
        return e;
    }

    // Converte RawTransactionEntity (persistência) para RawTransaction (domínio) reconstruindo value objects.
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
                e.getCreatedAt()
        );
    }

    // IMPORT FILE

    // Converte ImportFile (domínio) para ImportFileEntity aplicando sourceType derivado do tipo e defaults de timestamps.
    static ImportFileEntity toEntity(ImportFile file) {
        Objects.requireNonNull(file, "file must not be null");

        ImportFileEntity e = new ImportFileEntity();
        e.setId(file.id());

        e.setOriginalFilename(file.originalFileName());
        e.setStoredFilename(file.storedPath());
        e.setFileType(file.type());

        // >>> CORREÇÃO PRINCIPAL (Opção 1)
        e.setSourceType(resolveSourceType(file.type()));

        e.setContentType(null);

        e.setSizeBytes(file.sizeBytes());
        e.setUploadedAt(file.uploadedAt());

        Instant created = file.uploadedAt() != null
                ? file.uploadedAt()
                : Instant.now();

        e.setCreatedAt(created);

        return e;
    }

    // Converte ImportFileEntity (persistência) para ImportFile (domínio) preservando metadados do arquivo.
    static ImportFile toDomain(ImportFileEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return new ImportFile(
                e.getId(),
                e.getOriginalFilename(),
                e.getStoredFilename(),
                e.getFileType(),
                e.getSizeBytes(),
                e.getUploadedAt()
        );
    }

    // Resolve o TransactionSourceType a partir do ImportFileType para manter consistência de origem no domínio.
    private static TransactionSourceType resolveSourceType(ImportFileType type) {
        if (type == null) {
            // fallback defensivo
            return TransactionSourceType.MANUAL_ENTRY;
        }

        return switch (type) {
            case CSV -> TransactionSourceType.FILE_CSV;
            case OFX -> TransactionSourceType.FILE_OFX;
            case EVENT -> TransactionSourceType.KAFKA_EVENT;
        };
    }

    // IMPORT JOB

    // Mapeia ImportJob (domínio) para ImportJobEntity associando o ImportFileEntity e persistindo contadores/status.
    static ImportJobEntity toEntity(ImportJob job, ImportFileEntity fileEntity) {
        Objects.requireNonNull(job, "job must not be null");
        Objects.requireNonNull(fileEntity, "fileEntity must not be null");

        ImportJobEntity e = new ImportJobEntity();
        e.setId(job.id());
        e.setImportFile(fileEntity);
        e.setStatus(job.status());
        e.setTotalRecords(job.totalRecords());
        e.setProcessedRecords(job.processedRecords());
        e.setSuccessCount(job.successCount());
        e.setErrorCount(job.errorCount());
        e.setStartedAt(job.startedAt());
        e.setFinishedAt(job.finishedAt());
        e.setCreatedAt(job.createdAt());
        e.setUpdatedAt(job.updatedAt());
        return e;
    }

    // Converte ImportJobEntity (persistência) para ImportJob (domínio) garantindo a presença do ImportFile.
    static ImportJob toDomain(ImportJobEntity e) {
        Objects.requireNonNull(e, "entity must not be null");
        ImportFileEntity file = Objects.requireNonNull(
                e.getImportFile(), "importFile must not be null"
        );

        ImportJobStatus status = e.getStatus();

        return new ImportJob(
                e.getId(),
                file.getId(),
                status,
                e.getTotalRecords(),
                e.getProcessedRecords(),
                e.getSuccessCount(),
                e.getErrorCount(),
                e.getStartedAt(),
                e.getFinishedAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // IMPORT ERROR

    // Converte ImportError (domínio) para ImportErrorEntity vinculando o ImportJobEntity e persistindo detalhes do erro.
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

    // Converte ImportErrorEntity (persistência) para ImportError (domínio) preservando vínculo com o ImportJob.
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
