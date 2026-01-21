package br.com.ecofy.ms_ingestion.core.domain.valueobject;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

public final class TransactionDate {

    private final LocalDate value;

    // Encapsula a data da transação garantindo que o valor seja obrigatório (não nulo).
    public TransactionDate(LocalDate value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    // Retorna o LocalDate encapsulado.
    public LocalDate value() {
        return value;
    }

    // Converte a data para o início do dia em UTC (ZonedDateTime) para padronização temporal.
    public ZonedDateTime atStartOfDayUtc() {
        return value.atStartOfDay(ZoneOffset.UTC);
    }

    // Serializa a data no formato ISO-8601 (yyyy-MM-dd).
    @Override
    public String toString() {
        return value.toString();
    }

    // Factory method para criar TransactionDate com validação de não-nulo.
    public static TransactionDate of(LocalDate value) {
        Objects.requireNonNull(value, "value must not be null");
        return new TransactionDate(value);
    }

}
