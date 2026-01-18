package br.com.ecofy.ms_categorization.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "cat_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_cat_tx_job_external", columnNames = {"import_job_id", "external_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "import_job_id", columnDefinition = "uuid", nullable = false)
    private UUID importJobId;

    @Column(name = "external_id", nullable = false, length = 120)
    private String externalId;

    @Column(name = "description", nullable = false, length = 512)
    private String description;

    @Column(name = "merchant_norm", nullable = false, length = 512)
    private String merchantNormalized;

    @Column(name = "tx_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "category_id", columnDefinition = "uuid")
    private UUID categoryId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
