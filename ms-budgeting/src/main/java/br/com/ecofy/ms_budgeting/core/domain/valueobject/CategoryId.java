package br.com.ecofy.ms_budgeting.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) {

    // Valida e garante que o UUID da categoria não seja nulo ao criar o value object.
    public CategoryId {
        Objects.requireNonNull(value, "value must not be null");
    }

}
