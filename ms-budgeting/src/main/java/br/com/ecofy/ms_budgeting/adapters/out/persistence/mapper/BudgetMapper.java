package br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetEntity;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.BudgetKey;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.CategoryId;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Period;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.UserId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

// Centraliza a conversão entre orçamentos de domínio e entidades persistidas.
public final class BudgetMapper {

    private BudgetMapper() {}

    // Converte a entidade persistida em orçamento e valida sua chave natural.
    public static Budget toDomain(BudgetEntity e) {
        if (e == null) return null;

        UUID id = requireNonNull(e.getId(), "id");
        UUID userId = requireNonNull(e.getUserId(), "userId");
        UUID categoryId = requireNonNull(e.getCategoryId(), "categoryId");

        var period = new Period(
                requireNonNull(e.getPeriodStart(), "periodStart"),
                requireNonNull(e.getPeriodEnd(), "periodEnd")
        );

        var key = new BudgetKey(
                new UserId(userId),
                new CategoryId(categoryId),
                period
        );

        Currency currency = parseCurrency(e.getCurrency());
        var limit = new Money(requireNonNull(e.getLimitAmount(), "limitAmount"), currency);

        Instant createdAt = e.getCreatedAt() != null ? e.getCreatedAt() : Instant.now();
        Instant updatedAt = e.getUpdatedAt() != null ? e.getUpdatedAt() : createdAt;

        String expectedNaturalKey = key.asNaturalKey();
        if (e.getNaturalKey() == null || e.getNaturalKey().isBlank()) {
            throw new IllegalStateException("naturalKey must not be blank");
        }
        if (!expectedNaturalKey.equals(e.getNaturalKey())) {
            throw new IllegalStateException(
                    "naturalKey mismatch. expected=" + expectedNaturalKey + " persisted=" + e.getNaturalKey()
            );
        }

        return new Budget(
                id,
                key,
                requireNonNull(e.getPeriodType(), "periodType"),
                limit,
                requireNonNull(e.getStatus(), "status"),
                createdAt,
                updatedAt,
                e.getVersion()
        );
    }

    // Converte o orçamento em entidade persistível e define seus metadados.
    public static BudgetEntity toEntity(Budget b) {
        Objects.requireNonNull(b, "budget must not be null");
        Objects.requireNonNull(b.getKey(), "budget.key must not be null");
        Objects.requireNonNull(b.getLimit(), "budget.limit must not be null");

        String naturalKey = b.getKey().asNaturalKey();

        Instant createdAt = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.now();
        Instant updatedAt = b.getUpdatedAt() != null ? b.getUpdatedAt() : createdAt;

        LocalDate archivedAt = (b.getStatus() == BudgetStatus.ARCHIVED)
                ? updatedAt.atZone(ZoneOffset.UTC).toLocalDate()
                : null;

        return BudgetEntity.builder()
                .id(requireNonNull(b.getId(), "id"))
                .userId(requireNonNull(b.getKey().userId(), "key.userId").value())
                .categoryId(requireNonNull(b.getKey().categoryId(), "key.categoryId").value())
                .periodType(requireNonNull(b.getPeriodType(), "periodType"))
                .periodStart(requireNonNull(b.getKey().period(), "key.period").start())
                .periodEnd(requireNonNull(b.getKey().period(), "key.period").end())
                .limitAmount(requireNonNull(b.getLimit().amount(), "limit.amount"))
                .currency(requireNonNull(b.getLimit().currency(), "limit.currency").getCurrencyCode())
                .status(requireNonNull(b.getStatus(), "status"))
                .archivedAt(archivedAt)
                .naturalKey(naturalKey)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(b.getVersion())
                .build();
    }

    private static Currency parseCurrency(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Field 'currency' must not be blank");
        }
        try {
            return Currency.getInstance(code.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid currency code: " + code, ex);
        }
    }

    private static <T> T requireNonNull(T v, String field) {
        return Objects.requireNonNull(v, field + " must not be null");
    }
}
