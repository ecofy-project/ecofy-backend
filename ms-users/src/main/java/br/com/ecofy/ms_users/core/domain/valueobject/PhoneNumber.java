package br.com.ecofy.ms_users.core.domain.valueobject;

import java.util.Objects;

public record PhoneNumber(String value) {

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("phone must not be blank");
        }
    }

    public static PhoneNumber of(String v) {
        return new PhoneNumber(Objects.requireNonNull(v, "phone must not be null"));
    }
}
