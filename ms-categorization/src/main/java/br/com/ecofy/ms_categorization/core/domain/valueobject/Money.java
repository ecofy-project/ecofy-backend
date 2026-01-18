package br.com.ecofy.ms_categorization.core.domain.valueobject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public final class Money {

    private final BigDecimal amount;
    private final Currency currency;

    // Cria o value object Money validando amount e resolvendo a moeda a partir do código ISO-4217.
    public Money(BigDecimal amount, String currency) {
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.currency = Currency.getInstance(Objects.requireNonNull(currency, "currency must not be null"));
    }

    // Retorna o valor monetário (amount) do objeto.
    public BigDecimal getAmount() {
        return amount;
    }

    // Retorna a moeda (Currency) associada ao valor.
    public Currency getCurrency() {
        return currency;
    }

    // Informa o sinal do amount (-1, 0, 1) para apoiar regras/validações.
    public int sign() {
        return amount.signum();
    }

    // Define igualdade por valor considerando amount e currency (value object).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.equals(money.amount) &&
                currency.equals(money.currency);
    }

    // Gera hash consistente com equals para uso em coleções.
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    // Representa o Money em formato legível para logs/debug.
    @Override
    public String toString() {
        return "Money[" +
                "amount=" + amount +
                ", currency=" + currency +
                ']';
    }

}
