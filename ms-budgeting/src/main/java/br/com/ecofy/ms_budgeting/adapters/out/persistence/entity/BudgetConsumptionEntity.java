package br.com.ecofy.ms_budgeting.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "budget_consumption",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_budget_consumption_budget_refdate",
                columnNames = {"budget_id", "reference_date"}
        )
)
public class BudgetConsumptionEntity {

    @Id
    private UUID id;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    // Data de referência usada para retenção/limpeza (ex.: period_end) e suporte ao deleteByReferenceDateLessThanEqual.
    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "consumed_cents", nullable = false)
    private long consumedCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "source", nullable = false, length = 40)
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
