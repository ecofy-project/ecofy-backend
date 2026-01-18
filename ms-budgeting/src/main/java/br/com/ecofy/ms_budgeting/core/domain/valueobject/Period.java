package br.com.ecofy.ms_budgeting.core.domain.valueobject;

import java.time.LocalDate;
import java.util.Objects;

public record Period(LocalDate start, LocalDate end) {

    // Valida e garante que o período tenha datas não nulas e que o fim não seja anterior ao início.
    public Period {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Period end must be >= start");
        }
    }

    // Verifica se uma data está dentro do período (inclusivo em start e end).
    public boolean contains(LocalDate date) {
        return (date.isEqual(start) || date.isAfter(start)) && (date.isEqual(end) || date.isBefore(end));
    }

}
