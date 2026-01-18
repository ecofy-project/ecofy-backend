package br.com.ecofy.ms_categorization.core.domain;

import br.com.ecofy.ms_categorization.core.domain.valueobject.Merchant;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

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

    // Representa uma transação importada (normalizada) com metadados de origem, valor e auditoria.
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

    // Retorna o identificador único da transação.
    public UUID getId() {
        return id;
    }

    // Retorna o identificador do job de importação que originou a transação.
    public UUID getImportJobId() {
        return importJobId;
    }

    // Retorna o identificador externo (chave do provedor/arquivo) para deduplicação e rastreio.
    public String getExternalId() {
        return externalId;
    }

    // Retorna a descrição bruta da transação (texto original).
    public String getDescription() {
        return description;
    }

    // Retorna o merchant normalizado derivado da descrição para suportar regras de categorização.
    public Merchant getMerchant() {
        return merchant;
    }

    // Retorna a data da transação para agregações por período e relatórios.
    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    // Retorna o valor monetário (amount/currency) da transação.
    public Money getMoney() {
        return money;
    }

    // Retorna o tipo de origem (ex.: CSV, OFX, KAFKA) para auditoria e diagnóstico.
    public String getSourceType() {
        return sourceType;
    }

    // Retorna a categoria aplicada (se houver) após categorização manual/automática.
    public UUID getCategoryId() {
        return categoryId;
    }

    // Retorna o timestamp de criação da transação persistida.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Retorna o timestamp da última atualização da transação persistida.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Cria uma nova instância imutável da transação aplicando categoryId e atualizando updatedAt.
    public Transaction withCategory(UUID categoryId, Instant now) {
        return new Transaction(
                id, importJobId, externalId, description, merchant, transactionDate, money, sourceType,
                categoryId, createdAt, now
        );
    }

    // Compara transações por valor (campos relevantes) para consistência em coleções e testes.
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

    // Gera hash consistente com equals para uso em estruturas baseadas em hashing.
    @Override
    public int hashCode() {
        return Objects.hash(
                id, importJobId, externalId, description, merchant, transactionDate,
                money, sourceType, categoryId, createdAt, updatedAt
        );
    }

    // Fornece uma representação textual completa da transação para logs e debug.
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
