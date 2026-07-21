package br.com.ecofy.auth.core.domain.keys;

import java.time.Instant;
import java.util.Objects;

// Representa os metadados utilizados no ciclo de vida de uma chave de assinatura.
public record SigningKeyMetadata(
        String kid,
        String algorithm,
        Status status,
        Instant activeFrom,
        Instant retireAt,
        Instant expiresAt
) {

    // Define os estados permitidos durante a rotação da chave.
    public enum Status {
        ACTIVE,
        RETIRING
    }

    public SigningKeyMetadata {
        Objects.requireNonNull(
                kid,
                "kid must not be null"
        );
        Objects.requireNonNull(
                algorithm,
                "algorithm must not be null"
        );
        Objects.requireNonNull(
                status,
                "status must not be null"
        );

        if (kid.isBlank()) {
            throw new IllegalArgumentException(
                    "kid must not be blank"
            );
        }
    }

    // Valida se a chave permanece disponível para verificar tokens.
    public boolean isValidForVerificationAt(Instant now) {
        return expiresAt == null
                || now.isBefore(expiresAt);
    }
}
