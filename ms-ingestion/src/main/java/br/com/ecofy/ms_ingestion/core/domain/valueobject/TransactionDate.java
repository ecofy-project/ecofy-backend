package br.com.ecofy.ms_ingestion.core.domain.valueobject;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

// Centraliza a representação da data de uma transação.
public final class TransactionDate {

    private final LocalDate value;

    public TransactionDate(LocalDate value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public LocalDate value() {
        return value;
    }

    // Converte a data para o início do dia em UTC.
    public ZonedDateTime atStartOfDayUtc() {
        return value.atStartOfDay(ZoneOffset.UTC);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public static TransactionDate of(LocalDate value) {
        Objects.requireNonNull(value, "value must not be null");
        return new TransactionDate(value);
    }

}
