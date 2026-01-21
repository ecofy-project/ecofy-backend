package br.com.ecofy.ms_ingestion.adapters.out.persistence.entity;

import br.com.ecofy.ms_ingestion.core.domain.enums.ImportErrorType;
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
@Table(name = "import_error")
public class ImportErrorEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJobEntity importJob;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "raw_line", columnDefinition = "text")
    private String rawLine;

    @Column(name = "message", length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", length = 50, nullable = false)
    private ImportErrorType errorType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
