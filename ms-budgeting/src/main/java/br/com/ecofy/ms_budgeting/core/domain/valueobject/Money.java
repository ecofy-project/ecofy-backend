package br.com.ecofy.ms_budgeting.core.domain.valueobject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    // Valida e garante que o valor monetário e a moeda não sejam nulos ao criar o value object.
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }

    // Cria uma instância de Money com valor zero para a moeda informada.
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // Soma dois valores monetários exigindo que ambos estejam na mesma moeda.
    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    // Subtrai dois valores monetários exigindo que ambos estejam na mesma moeda.
    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    // Indica se o valor monetário é negativo.
    public boolean isNegative() {
        return amount.signum() < 0;
    }

    // Valida que o outro Money existe e possui a mesma moeda desta instância.
    public void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");

        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

}
