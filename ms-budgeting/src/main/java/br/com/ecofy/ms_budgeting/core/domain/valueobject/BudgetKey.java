package br.com.ecofy.ms_budgeting.core.domain.valueobject;

import java.util.Objects;

public record BudgetKey(UserId userId, CategoryId categoryId, Period period) {

    // Valida e garante que os componentes da chave natural do budget não sejam nulos.
    public BudgetKey {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        Objects.requireNonNull(period, "period must not be null");
    }

    // Gera a naturalKey única do budget (user + categoria + período) para deduplicação/unique constraint.
    public String asNaturalKey() {
        return userId.value() + ":" + categoryId.value() + ":" + period.start() + ":" + period.end();
    }

}
