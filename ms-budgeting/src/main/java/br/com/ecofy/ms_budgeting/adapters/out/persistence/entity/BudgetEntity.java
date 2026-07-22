package br.com.ecofy.ms_budgeting.adapters.out.persistence.entity;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
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
@Table(
        name = "budgets",
        uniqueConstraints = @UniqueConstraint(name = "uk_budget_natural", columnNames = {"natural_key"})
)
public class BudgetEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "category_id", nullable = false, updatable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private BudgetPeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BudgetStatus status;

    // Data preenchida ao arquivar (ARCHIVED) e usada pelo cleanup para expurgo de arquivados antigos.
    @Column(name = "archived_at")
    private LocalDate archivedAt;

    @Column(name = "natural_key", nullable = false, length = 200, updatable = false)
    private String naturalKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Optimistic locking (ECO-11). @Version também altera o isNew do Spring Data: version null
    // = novo (insert); version preenchida = existente (merge com checagem de conflito).
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // Construtor de compatibilidade (sem version) — preserva chamadas anteriores ao ECO-11.
    // version=null mantém a semântica de "entidade nova" (insert) do Spring Data.
    public BudgetEntity(UUID id, UUID userId, UUID categoryId, BudgetPeriodType periodType,
                        LocalDate periodStart, LocalDate periodEnd, BigDecimal limitAmount, String currency,
                        BudgetStatus status, LocalDate archivedAt, String naturalKey,
                        Instant createdAt, Instant updatedAt) {
        this(id, userId, categoryId, periodType, periodStart, periodEnd, limitAmount, currency,
                status, archivedAt, naturalKey, createdAt, updatedAt, null);
    }

    // Inicializa id e timestamps e normaliza campos antes de inserir no banco.
    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        var now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;

        if (currency != null) currency = currency.trim().toUpperCase();
        if (naturalKey != null) naturalKey = naturalKey.trim();
    }

    // Atualiza timestamp e normaliza campos antes de atualizar no banco.
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        if (currency != null) currency = currency.trim().toUpperCase();
    }

}
