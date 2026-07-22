package br.com.ecofy.ms_budgeting.core.domain.valueobject;

import java.util.Objects;

// Compõe a chave natural que identifica unicamente um orçamento.
public record BudgetKey(UserId userId, CategoryId categoryId, Period period) {

    public BudgetKey {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        Objects.requireNonNull(period, "period must not be null");
    }

    // Gera a representação textual utilizada na deduplicação.
    public String asNaturalKey() {
        return userId.value() + ":" + categoryId.value() + ":" + period.start() + ":" + period.end();
    }
}
