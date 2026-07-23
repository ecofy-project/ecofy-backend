package br.com.ecofy.ms_ingestion.adapters.out.persistence.entity;

import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "raw_transaction")
public class RawTransactionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJobEntity importJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_file_id", nullable = false)
    private ImportFileEntity importFile;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50, nullable = false)
    private TransactionSourceType sourceType;

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    @Column(name = "row_hash", length = 128, nullable = false, updatable = false)
    private String rowHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
