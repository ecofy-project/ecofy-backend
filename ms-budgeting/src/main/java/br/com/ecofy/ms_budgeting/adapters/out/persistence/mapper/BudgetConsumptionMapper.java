package br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetConsumptionEntity;
import br.com.ecofy.ms_budgeting.core.domain.BudgetConsumption;
import br.com.ecofy.ms_budgeting.core.domain.enums.ConsumptionSource;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

// Centraliza a conversão entre o consumo de orçamento e sua representação persistida.
public final class BudgetConsumptionMapper {

    private static final int SCALE = 2;

    private BudgetConsumptionMapper() {}

    // Converte o consumo de orçamento em uma entidade persistível.
    public static BudgetConsumptionEntity toEntity(BudgetConsumption d) {
        Objects.requireNonNull(d, "domain must not be null");
        Objects.requireNonNull(d.getConsumed(), "domain.consumed must not be null");

        Currency currency = d.getConsumed().currency();
        String currencyCode = currency.getCurrencyCode();

        long consumedCents = toCents(d.getConsumed().amount());

        return BudgetConsumptionEntity.builder()
                .id(d.getId())
                .budgetId(d.getBudgetId())
                .referenceDate(d.getPeriodEnd())
                .periodStart(d.getPeriodStart())
                .periodEnd(d.getPeriodEnd())
                .consumedCents(consumedCents)
                .currency(currencyCode)
                .source(d.getSource().name())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    // Converte a entidade persistida em um consumo de orçamento.
    public static BudgetConsumption toDomain(BudgetConsumptionEntity e) {
        if (e == null) return null;

        Currency currency = Currency.getInstance(requireNonBlank(e.getCurrency(), "currency"));
        Money consumed = fromCents(e.getConsumedCents(), currency);

        Instant createdAt = e.getCreatedAt() != null ? e.getCreatedAt() : Instant.now();
        Instant updatedAt = e.getUpdatedAt() != null ? e.getUpdatedAt() : createdAt;

        return new BudgetConsumption(
                requireNonNull(e.getId(), "id"),
                requireNonNull(e.getBudgetId(), "budgetId"),
                requireNonNull(e.getPeriodStart(), "periodStart"),
                requireNonNull(e.getPeriodEnd(), "periodEnd"),
                consumed,
                ConsumptionSource.valueOf(requireNonBlank(e.getSource(), "source")),
                createdAt,
                updatedAt
        );
    }

    // Cria um consumo inicial sem valor para o período informado.
    public static BudgetConsumption newEmpty(
            UUID budgetId,
            java.time.LocalDate periodStart,
            java.time.LocalDate periodEnd,
            String currencyCode
    ) {
        Currency currency = Currency.getInstance(requireNonBlank(currencyCode, "currencyCode"));
        Instant now = Instant.now();

        return new BudgetConsumption(
                UUID.randomUUID(),
                requireNonNull(budgetId, "budgetId"),
                requireNonNull(periodStart, "periodStart"),
                requireNonNull(periodEnd, "periodEnd"),
                new Money(BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY), currency),
                ConsumptionSource.CATEGORIZED_TX,
                now,
                now
        );
    }

    private static Money fromCents(long cents, Currency currency) {
        BigDecimal amount = BigDecimal.valueOf(cents)
                .movePointLeft(SCALE)
                .setScale(SCALE, RoundingMode.UNNECESSARY);

        return new Money(amount, currency);
    }

    private static long toCents(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(SCALE, RoundingMode.HALF_UP);

        return scaled.movePointRight(SCALE).longValueExact();
    }

    private static <T> T requireNonNull(T v, String field) {
        return Objects.requireNonNull(v, field + " must not be null");
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return v.trim();
    }
}
