package br.com.ecofy.ms_budgeting.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

// Representa o identificador único de uma categoria.
public record CategoryId(UUID value) {

    public CategoryId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
