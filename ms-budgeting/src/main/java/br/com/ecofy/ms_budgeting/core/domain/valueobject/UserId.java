package br.com.ecofy.ms_budgeting.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    // Valida e garante que o identificador do usuário não seja nulo.
    public UserId {
        Objects.requireNonNull(value, "value must not be null");
    }

}
