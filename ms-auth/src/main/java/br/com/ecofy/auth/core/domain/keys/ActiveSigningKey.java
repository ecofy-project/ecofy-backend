package br.com.ecofy.auth.core.domain.keys;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

// Representa a chave ativa utilizada na assinatura de novos tokens.
public record ActiveSigningKey(
        SigningKeyMetadata metadata,
        RSAPrivateKey privateKey,
        RSAPublicKey publicKey
) {

    public ActiveSigningKey {
        Objects.requireNonNull(
                metadata,
                "metadata must not be null"
        );
        Objects.requireNonNull(
                privateKey,
                "privateKey must not be null"
        );
        Objects.requireNonNull(
                publicKey,
                "publicKey must not be null"
        );

        if (metadata.status()
                != SigningKeyMetadata.Status.ACTIVE) {
            throw new IllegalArgumentException(
                    "active signing key must have status ACTIVE"
            );
        }
    }

    public String kid() {
        return metadata.kid();
    }

    // Restringe a representação textual aos metadados não sensíveis.
    @Override
    public String toString() {
        return "ActiveSigningKey[kid="
                + metadata.kid()
                + ", alg="
                + metadata.algorithm()
                + "]";
    }
}
