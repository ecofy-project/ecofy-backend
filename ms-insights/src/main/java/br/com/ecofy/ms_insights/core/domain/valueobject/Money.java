package br.com.ecofy.ms_insights.core.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

// Centraliza operações e validações de valores monetários do domínio.
public record Money(BigDecimal amount, String currency) implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (currency.isBlank()) throw new IllegalArgumentException("currency must not be blank");
        amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    // Converte centavos para a representação monetária do domínio.
    public static Money ofCents(long cents, String currency) {
        return new Money(BigDecimal.valueOf(cents, SCALE), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // Converte o valor monetário para centavos.
    public long cents() {
        return amount.movePointRight(SCALE).setScale(0, ROUNDING).longValueExact();
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), currency);
    }

    public Money divide(long divisor) {
        if (divisor == 0) throw new IllegalArgumentException("divisor must not be zero");
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING), currency);
    }

    // Calcula a participação percentual em relação a uma base positiva.
    public BigDecimal percentageOf(Money base) {
        requireSameCurrency(base);
        if (base.amount.signum() <= 0) return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        return this.amount.multiply(BigDecimal.valueOf(100)).divide(base.amount, SCALE, RoundingMode.HALF_UP);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    // Valida a compatibilidade entre as moedas das operações.
    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }
}
