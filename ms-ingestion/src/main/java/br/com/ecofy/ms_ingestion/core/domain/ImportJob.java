package br.com.ecofy.ms_ingestion.core.domain;

import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ImportJob {

    private final UUID id;
    private final UUID importFileId;

    private ImportJobStatus status;

    private int totalRecords;
    private int processedRecords;
    private int successCount;
    private int errorCount;

    private Instant startedAt;
    private Instant finishedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public ImportJob(UUID id,
                     UUID importFileId,
                     ImportJobStatus status,
                     int totalRecords,
                     int processedRecords,
                     int successCount,
                     int errorCount,
                     Instant startedAt,
                     Instant finishedAt,
                     Instant createdAt,
                     Instant updatedAt) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.importFileId = Objects.requireNonNull(importFileId, "importFileId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.totalRecords = totalRecords;
        this.processedRecords = processedRecords;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNullElseGet(updatedAt, Instant::now);
    }

    // Factory usado no service
    public static ImportJob create(UUID importFileId) {
        Instant now = Instant.now();
        return new ImportJob(
                UUID.randomUUID(),
                importFileId,
                ImportJobStatus.PENDING,
                0,
                0,
                0,
                0,
                null,
                null,
                now,
                now
        );
    }

    // ======== Comportamentos de domínio usados no service ========

    public void markRunning() {
        this.status = ImportJobStatus.RUNNING;
        this.startedAt = this.startedAt == null ? Instant.now() : this.startedAt;
        this.updatedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = ImportJobStatus.COMPLETED;
        this.finishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markCompletedWithErrors() {
        this.status = ImportJobStatus.COMPLETED_WITH_ERRORS;
        this.finishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = ImportJobStatus.FAILED;
        this.finishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ======== Getters usados em mapper / DTO / service ========

    public UUID id() {
        return id;
    }

    public UUID importFileId() {
        return importFileId;
    }

    public ImportJobStatus status() {
        return status;
    }

    public int totalRecords() {
        return totalRecords;
    }

    public int processedRecords() {
        return processedRecords;
    }

    public int successCount() {
        return successCount;
    }

    public int errorCount() {
        return errorCount;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void updateCounts(int totalRecords,
                             int processedRecords,
                             int successCount,
                             int errorCount) {
        this.totalRecords = totalRecords;
        this.processedRecords = processedRecords;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.updatedAt = Instant.now();
    }

}
