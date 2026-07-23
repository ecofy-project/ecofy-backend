package br.com.ecofy.ms_categorization.core.domain.valueobject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

// Representa um valor monetário associado a uma moeda válida.
public final class Money {

    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, String currency) {
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.currency = Currency.getInstance(Objects.requireNonNull(currency, "currency must not be null"));
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public int sign() {
        return amount.signum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.equals(money.amount) &&
                currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return "Money[" +
                "amount=" + amount +
                ", currency=" + currency +
                ']';
    }
}
