package br.com.ecofy.ms_categorization.core.domain;

import br.com.ecofy.ms_categorization.core.domain.valueobject.Merchant;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

// Representa uma transação importada disponível para categorização.
public final class Transaction {

    private final UUID id;
    private final UUID importJobId;
    private final String externalId;
    private final String description;
    private final Merchant merchant;
    private final LocalDate transactionDate;
    private final Money money;
    private final String sourceType;
    private final UUID categoryId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Transaction(
            UUID id,
            UUID importJobId,
            String externalId,
            String description,
            Merchant merchant,
            LocalDate transactionDate,
            Money money,
            String sourceType,
            UUID categoryId,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.importJobId = Objects.requireNonNull(importJobId, "importJobId must not be null");
        this.externalId = Objects.requireNonNull(externalId, "externalId must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.merchant = Objects.requireNonNull(merchant, "merchant must not be null");
        this.transactionDate = Objects.requireNonNull(transactionDate, "transactionDate must not be null");
        this.money = Objects.requireNonNull(money, "money must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.categoryId = categoryId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getImportJobId() {
        return importJobId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getDescription() {
        return description;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public Money getMoney() {
        return money;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Aplica uma categoria preservando a imutabilidade da transação.
    public Transaction withCategory(UUID categoryId, Instant now) {
        return new Transaction(
                id, importJobId, externalId, description, merchant, transactionDate, money, sourceType,
                categoryId, createdAt, now
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return id.equals(that.id) &&
                importJobId.equals(that.importJobId) &&
                externalId.equals(that.externalId) &&
                description.equals(that.description) &&
                merchant.equals(that.merchant) &&
                transactionDate.equals(that.transactionDate) &&
                money.equals(that.money) &&
                sourceType.equals(that.sourceType) &&
                Objects.equals(categoryId, that.categoryId) &&
                createdAt.equals(that.createdAt) &&
                updatedAt.equals(that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, importJobId, externalId, description, merchant, transactionDate,
                money, sourceType, categoryId, createdAt, updatedAt
        );
    }

    @Override
    public String toString() {
        return "Transaction[" +
                "id=" + id +
                ", importJobId=" + importJobId +
                ", externalId=" + externalId +
                ", description=" + description +
                ", merchant=" + merchant +
                ", transactionDate=" + transactionDate +
                ", money=" + money +
                ", sourceType=" + sourceType +
                ", categoryId=" + categoryId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ']';
    }
}
