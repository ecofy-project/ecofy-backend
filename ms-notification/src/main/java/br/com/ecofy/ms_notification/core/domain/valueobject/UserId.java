package br.com.ecofy.ms_notification.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    // Garante que o identificador do usuário seja válido (não nulo) ao criar o value object.
    public UserId {
        Objects.requireNonNull(value, "userId must not be null");
    }

}
