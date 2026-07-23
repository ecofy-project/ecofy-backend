package br.com.ecofy.ms_categorization.adapters.out.persistence.mapper;

import br.com.ecofy.ms_categorization.adapters.out.persistence.entity.TransactionEntity;
import br.com.ecofy.ms_categorization.core.domain.Transaction;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Merchant;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Centraliza a conversão entre transações de domínio e entidades persistidas.
@Component
public class TransactionMapper {

    private final Clock clock;

    public TransactionMapper() {
        this(Clock.systemUTC());
    }

    public TransactionMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Converte a transação em uma entidade persistível.
    public TransactionEntity toEntity(Transaction d) {
        Objects.requireNonNull(d, "domain must not be null");
        Objects.requireNonNull(d.getMoney(), "domain.money must not be null");
        Objects.requireNonNull(d.getMerchant(), "domain.merchant must not be null");

        return TransactionEntity.builder()
                .id(d.getId())
                .importJobId(d.getImportJobId())
                .externalId(d.getExternalId())
                .description(d.getDescription())
                .merchantNormalized(d.getMerchant().getNormalized())
                .transactionDate(d.getTransactionDate())
                .amount(d.getMoney().getAmount())
                .currency(d.getMoney().getCurrency().getCurrencyCode())
                .sourceType(d.getSourceType())
                .categoryId(d.getCategoryId())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    // Converte a entidade persistida em uma transação.
    public Transaction toDomain(TransactionEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return new Transaction(
                nonNullOrThrow(e.getId(), "entity.id must not be null"),
                nonNullOrThrow(e.getImportJobId(), "entity.importJobId must not be null"),
                nonNullOrThrow(e.getExternalId(), "entity.externalId must not be null"),
                nonNullOrDefault(e.getDescription()),
                Merchant.of(e.getDescription()),
                nonNullOrThrow(e.getTransactionDate(), "entity.transactionDate must not be null"),
                new Money(
                        nonNullOrThrow(e.getAmount(), "entity.amount must not be null"),
                        nonNullOrThrow(e.getCurrency(), "entity.currency must not be null")
                ),
                nonNullOrThrow(e.getSourceType(), "entity.sourceType must not be null"),
                e.getCategoryId(),
                nonNullOrNow(e.getCreatedAt()),
                nonNullOrNow(e.getUpdatedAt())
        );
    }

    private UUID nonNullOrThrow(UUID v, String msg) {
        if (v == null) throw new IllegalStateException(msg);
        return v;
    }

    private <T> T nonNullOrThrow(T v, String msg) {
        if (v == null) throw new IllegalStateException(msg);
        return v;
    }

    private String nonNullOrDefault(String v) {
        return v == null ? "" : v;
    }

    private Instant nonNullOrNow(Instant v) {
        return v != null ? v : Instant.now(clock);
    }
}
