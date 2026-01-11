package br.com.ecofy.ms_users.core.domain.valueobject;

import java.util.Objects;

public record ExternalAuthId(String value) {
    public ExternalAuthId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("externalAuthId must not be blank");
        }
    }

    public static ExternalAuthId of(String v) {
        return new ExternalAuthId(Objects.requireNonNull(v, "externalAuthId must not be null"));
    }
}