package br.com.ecofy.ms_users.core.domain.valueobject;

import java.util.Objects;

public record PhoneNumber(String value) {

    // Valida e encapsula o telefone em um Value Object, garantindo não-nulo e não-vazio.
    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("phone must not be blank");
        }
    }

    // Fábrica estática para criar PhoneNumber a partir de uma string, aplicando null-check obrigatório.
    public static PhoneNumber of(String v) {
        return new PhoneNumber(Objects.requireNonNull(v, "phone must not be null"));
    }

}
