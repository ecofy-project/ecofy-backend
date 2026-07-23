package br.com.ecofy.ms_ingestion.core.domain;

import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.RowHash;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class RawTransaction {

    private final UUID id;
    private final UUID importJobId;
    private final String externalId;
    private final String description;
    private final TransactionDate date;
    private final Money amount;
    private final TransactionSourceType sourceType;

    // Guarda a chave estável derivada do conteúdo da linha, base da deduplicação no banco.
    private final String rowHash;

    private final Instant createdAt;

    public RawTransaction(UUID id,
                          UUID importJobId,
                          String externalId,
                          String description,
                          TransactionDate date,
                          Money amount,
                          TransactionSourceType sourceType,
                          String rowHash,
                          Instant createdAt) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.importJobId = Objects.requireNonNull(importJobId, "importJobId must not be null");
        this.externalId = externalId;
        this.description = description;
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.rowHash = Objects.requireNonNull(rowHash, "rowHash must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static RawTransaction create(UUID importJobId,
                                        String externalId,
                                        String description,
                                        TransactionDate date,
                                        Money amount,
                                        TransactionSourceType sourceType) {

        String rowHash = RowHash.of(externalId, description, date, amount);

        return new RawTransaction(
                UUID.randomUUID(),
                importJobId,
                externalId,
                description,
                date,
                amount,
                sourceType,
                rowHash,
                Instant.now()
        );
    }

    public UUID id() {
        return id;
    }

    public UUID importJobId() {
        return importJobId;
    }

    public String externalId() {
        return externalId;
    }

    public String description() {
        return description;
    }

    public TransactionDate date() {
        return date;
    }

    public Money amount() {
        return amount;
    }

    public TransactionSourceType sourceType() {
        return sourceType;
    }

    public String rowHash() {
        return rowHash;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
