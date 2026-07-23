package br.com.ecofy.ms_ingestion.adapters.out.persistence.entity;

import br.com.ecofy.ms_ingestion.core.domain.enums.ImportJobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "import_job")
public class ImportJobEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_file_id", nullable = false)
    private ImportFileEntity importFile;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ImportJobStatus status;

    @Column(name = "total_records")
    private int totalRecords;

    @Column(name = "processed_records")
    private int processedRecords;

    @Column(name = "success_count")
    private int successCount;

    @Column(name = "error_count")
    private int errorCount;

    @Column(name = "duplicate_records", nullable = false)
    private int duplicateRecords;

    @Column(name = "published_records", nullable = false)
    private int publishedRecords;

    // Quantos erros foram de fato PERSISTIDOS (<= errorCount quando houve truncamento).
    @Column(name = "recorded_errors", nullable = false)
    private int recordedErrors;

    @Column(name = "errors_truncated", nullable = false)
    private boolean errorsTruncated;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    // Motivo resumido e sanitizado. Stack trace nunca é persistido (§12).
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

}
