package br.com.ecofy.auth.core.domain.keys;

import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

// Representa uma chave pública disponível para a verificação de tokens.
public record VerificationKey(
        SigningKeyMetadata metadata,
        RSAPublicKey publicKey
) {

    public VerificationKey {
        Objects.requireNonNull(
                metadata,
                "metadata must not be null"
        );
        Objects.requireNonNull(
                publicKey,
                "publicKey must not be null"
        );
    }

    public String kid() {
        return metadata.kid();
    }
}
