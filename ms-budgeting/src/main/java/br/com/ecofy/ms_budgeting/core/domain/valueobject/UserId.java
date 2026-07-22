package br.com.ecofy.ms_budgeting.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

// Representa o identificador único de um usuário.
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
