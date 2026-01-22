package br.com.ecofy.ms_notification.core.domain.valueobject;

import java.util.Objects;

public record IdempotencyKey(String value) {

    // Valida e normaliza a chave de idempotência: impede null e strings em branco para garantir unicidade/rastreabilidade.
    public IdempotencyKey {
        Objects.requireNonNull(value, "idempotencyKey must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }

}
