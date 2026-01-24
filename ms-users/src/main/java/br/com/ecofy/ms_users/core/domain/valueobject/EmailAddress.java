package br.com.ecofy.ms_users.core.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern BASIC = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    // Valida e encapsula um e-mail em um Value Object, garantindo formato mínimo e imutabilidade.
    public EmailAddress {
        if (value == null || value.isBlank() || !BASIC.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid email");
        }
    }

    // Fábrica estática para criar EmailAddress a partir de uma string, aplicando null-check obrigatório.
    public static EmailAddress of(String v) {
        return new EmailAddress(Objects.requireNonNull(v, "email must not be null"));
    }

}
