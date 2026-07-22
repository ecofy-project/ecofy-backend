package br.com.ecofy.ms_budgeting.core.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

// Representa valores monetários com escala e arredondamento padronizados.
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = amount.setScale(SCALE, ROUNDING);
    }

    // Cria um valor monetário a partir do código da moeda.
    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        return new Money(amount, Currency.getInstance(currencyCode.trim().toUpperCase()));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // Soma valores que utilizam a mesma moeda.
    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    // Subtrai valores que utilizam a mesma moeda.
    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    // Calcula o percentual do valor em relação a um limite da mesma moeda.
    public BigDecimal percentageOf(Money limit) {
        requireSameCurrency(limit);
        if (limit.amount.signum() <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return this.amount
                .multiply(BigDecimal.valueOf(100))
                .divide(limit.amount, SCALE, RoundingMode.HALF_UP);
    }

    // Compara valores que utilizam a mesma moeda.
    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    public void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");

        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }
}
