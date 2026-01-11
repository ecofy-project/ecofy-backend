package br.com.ecofy.ms_users.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "userId must not be null");
    }

    public static UserId of(UUID v) {
        return new UserId(v);
    }
}