package br.com.ecofy.ms_ingestion.core.domain;

import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// Representa o job de importação de um arquivo, com ciclo de vida guardado contra transições inconsistentes.
public class ImportJob {

    // Declara as transições de status permitidas, exigindo operação explícita para reabrir um job terminal.
    private static final Map<ImportJobStatus, Set<ImportJobStatus>> ALLOWED_TRANSITIONS = Map.of(
            ImportJobStatus.PENDING, Set.of(ImportJobStatus.RUNNING, ImportJobStatus.FAILED),
            ImportJobStatus.RUNNING, Set.of(
                    ImportJobStatus.COMPLETED,
                    ImportJobStatus.COMPLETED_WITH_ERRORS,
                    ImportJobStatus.FAILED),
            ImportJobStatus.COMPLETED, Set.of(),
            ImportJobStatus.COMPLETED_WITH_ERRORS, Set.of(),
            ImportJobStatus.FAILED, Set.of()
    );

    private final UUID id;
    private final UUID importFileId;
    private final UUID userId;

    private ImportJobStatus status;

    private int totalRecords;
    private int processedRecords;
    private int successCount;
    private int errorCount;
    private int duplicateRecords;
    private int publishedRecords;
    private int recordedErrors;
    private boolean errorsTruncated;

    private String failureCode;
    private String failureReason;

    private Instant deadlineAt;
    private final String correlationId;

    private Instant startedAt;
    private Instant finishedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public ImportJob(UUID id,
                     UUID importFileId,
                     UUID userId,
                     ImportJobStatus status,
                     int totalRecords,
                     int processedRecords,
                     int successCount,
                     int errorCount,
                     int duplicateRecords,
                     int publishedRecords,
                     int recordedErrors,
                     boolean errorsTruncated,
                     String failureCode,
                     String failureReason,
                     Instant deadlineAt,
                     String correlationId,
                     Instant startedAt,
                     Instant finishedAt,
                     Instant createdAt,
                     Instant updatedAt) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.importFileId = Objects.requireNonNull(importFileId, "importFileId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.totalRecords = totalRecords;
        this.processedRecords = processedRecords;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.duplicateRecords = duplicateRecords;
        this.publishedRecords = publishedRecords;
        this.recordedErrors = recordedErrors;
        this.errorsTruncated = errorsTruncated;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.deadlineAt = deadlineAt;
        this.correlationId = correlationId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNullElseGet(updatedAt, Instant::now);
    }

    public static ImportJob create(UUID importFileId, UUID userId, String correlationId) {
        Instant now = Instant.now();
        return new ImportJob(
                UUID.randomUUID(),
                importFileId,
                userId,
                ImportJobStatus.PENDING,
                0, 0, 0, 0, 0, 0, 0,
                false,
                null,
                null,
                null,
                correlationId,
                null,
                null,
                now,
                now
        );
    }

    // ======== Ciclo de vida ========

    // Marca o job em execução e grava o deadline, permitindo parada em ponto seguro.
    public void markRunning(Duration processingTimeout) {
        transitionTo(ImportJobStatus.RUNNING);
        Instant now = Instant.now();
        this.startedAt = this.startedAt == null ? now : this.startedAt;
        this.deadlineAt = processingTimeout == null ? null : now.plus(processingTimeout);
        this.updatedAt = now;
    }

    public void markCompleted() {
        transitionTo(ImportJobStatus.COMPLETED);
        finish();
    }

    public void markCompletedWithErrors() {
        transitionTo(ImportJobStatus.COMPLETED_WITH_ERRORS);
        finish();
    }

    public void markFailed(String failureCode, String failureReason) {
        transitionTo(ImportJobStatus.FAILED);
        this.failureCode = failureCode;
        this.failureReason = truncateReason(failureReason);
        finish();
    }

    // Reabre um job terminal para reprocessamento, única saída explícita de um estado final.
    public void reopenForRetry() {
        if (!isTerminal()) {
            throw new IllegalStateTransitionException(status, ImportJobStatus.PENDING);
        }
        this.status = ImportJobStatus.PENDING;
        this.finishedAt = null;
        this.failureCode = null;
        this.failureReason = null;
        this.deadlineAt = null;
        resetCounts();
        this.updatedAt = Instant.now();
    }

    public boolean isTerminal() {
        return ALLOWED_TRANSITIONS.get(status).isEmpty();
    }

    // Informa se o deadline já expirou, verificado entre lotes para interromper em ponto seguro.
    public boolean isPastDeadline(Instant now) {
        return deadlineAt != null && now.isAfter(deadlineAt);
    }

    private void transitionTo(ImportJobStatus target) {
        Set<ImportJobStatus> allowed = ALLOWED_TRANSITIONS.get(status);
        if (allowed == null || !allowed.contains(target)) {
            throw new IllegalStateTransitionException(status, target);
        }
        this.status = target;
    }

    private void finish() {
        Instant now = Instant.now();
        this.finishedAt = now;
        this.updatedAt = now;
    }

    private void resetCounts() {
        this.totalRecords = 0;
        this.processedRecords = 0;
        this.successCount = 0;
        this.errorCount = 0;
        this.duplicateRecords = 0;
        this.publishedRecords = 0;
        this.recordedErrors = 0;
        this.errorsTruncated = false;
    }

    // Mantém failureReason dentro do limite da coluna (500) sem depender do banco truncar.
    private static String truncateReason(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= 500 ? reason : reason.substring(0, 497) + "...";
    }

    // ======== Contadores ========

    // Acumula o resultado de um lote, preservando o progresso de um job interrompido.
    public void addBatchResult(int valid, int invalid, int duplicated, int published, int recorded) {
        this.successCount += valid;
        this.errorCount += invalid;
        this.duplicateRecords += duplicated;
        this.publishedRecords += published;
        this.recordedErrors += recorded;
        this.totalRecords += valid + invalid + duplicated;
        this.processedRecords += valid + invalid + duplicated;
        this.updatedAt = Instant.now();
    }

    public void markErrorsTruncated() {
        this.errorsTruncated = true;
        this.updatedAt = Instant.now();
    }

    // ======== Getters ========

    public UUID id() {
        return id;
    }

    public UUID importFileId() {
        return importFileId;
    }

    public UUID userId() {
        return userId;
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

    public int duplicateRecords() {
        return duplicateRecords;
    }

    public int publishedRecords() {
        return publishedRecords;
    }

    public int recordedErrors() {
        return recordedErrors;
    }

    public boolean errorsTruncated() {
        return errorsTruncated;
    }

    public String failureCode() {
        return failureCode;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant deadlineAt() {
        return deadlineAt;
    }

    public String correlationId() {
        return correlationId;
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

    // Sinaliza transição de estado inválida do job, indicando erro de fluxo interno.
    public static class IllegalStateTransitionException extends IllegalStateException {
        public IllegalStateTransitionException(ImportJobStatus from, ImportJobStatus to) {
            super("Illegal ImportJob transition: " + from + " -> " + to);
        }
    }
}
