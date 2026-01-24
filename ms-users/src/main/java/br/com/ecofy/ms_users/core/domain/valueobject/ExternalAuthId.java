package br.com.ecofy.ms_users.core.domain.valueobject;

import java.util.Objects;

public record ExternalAuthId(String value) {

    // Valida e encapsula o identificador externo de autenticação em um Value Object, garantindo não-nulo e não-vazio.
    public ExternalAuthId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("externalAuthId must not be blank");
        }
    }

    // Fábrica estática para criar ExternalAuthId a partir de uma string, aplicando null-check obrigatório.
    public static ExternalAuthId of(String v) {
        return new ExternalAuthId(Objects.requireNonNull(v, "externalAuthId must not be null"));
    }

}
