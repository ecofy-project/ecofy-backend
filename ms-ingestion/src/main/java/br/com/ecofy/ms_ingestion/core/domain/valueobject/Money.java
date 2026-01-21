package br.com.ecofy.ms_ingestion.core.domain.valueobject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public final class Money {

    private final BigDecimal amount;
    private final String currency;

    // Cria um Money com defaults defensivos (amount=0 e currency=BRL) e normaliza o código da moeda.
    public Money(BigDecimal amount, String currency) {
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.currency = (currency == null || currency.isBlank())
                ? "BRL"
                : currency.trim().toUpperCase();
    }

    // Cria um Money a partir de java.util.Currency, aplicando defaults defensivos (amount=0 e currency=BRL).
    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.currency = (currency == null)
                ? "BRL"
                : currency.getCurrencyCode();
    }

    // Retorna o valor monetário (BigDecimal) do objeto.
    public BigDecimal amount() {
        return amount;
    }

    // Retorna o código ISO da moeda (String) do objeto.
    public String currency() {
        return currency;
    }

    // Retorna uma nova instância com o valor negado, mantendo a mesma moeda.
    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    // Soma dois valores monetários da mesma moeda, validando compatibilidade de currency.
    public Money add(Money other) {
        Objects.requireNonNull(other, "other must not be null");

        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s".formatted(this.currency, other.currency)
            );
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    // Serializa o valor no formato "<CURRENCY> <AMOUNT>" para logs/debug.
    @Override
    public String toString() {
        return currency + " " + amount;
    }

    // Cria Money com moeda padrão BRL, exigindo amount não nulo.
    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        return new Money(amount, "BRL");
    }

    // Cria Money com moeda informada, exigindo amount não nulo e normalizando currency internamente.
    public static Money of(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        return new Money(amount, currency);
    }
}
