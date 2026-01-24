package br.com.ecofy.ms_users.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    // Encapsula o UUID do usuário em um Value Object, garantindo que o identificador não seja nulo.
    public UserId {
        Objects.requireNonNull(value, "userId must not be null");
    }

    // Fábrica estática para criar UserId a partir de um UUID.
    public static UserId of(UUID v) {
        return new UserId(v);
    }

}
