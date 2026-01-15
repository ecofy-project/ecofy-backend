package br.com.ecofy.auth.core.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

// Representação de uma chave pública exposta via JWKS no domínio.
public class JwkKey {

    // Constante do valor JWKS "use" para chaves de assinatura (sig).
    public static final String USE_SIGNING    = "sig";

    // Constante do valor JWKS "use" para chaves de criptografia (enc).
    public static final String USE_ENCRYPTION = "enc";

    private final String keyId;
    private final String publicKeyPem;
    private final String algorithm; // ex.: "RS256"
    private final String use; // "sig" ou "enc"
    private final Instant createdAt;
    private final boolean active;

    // Constrói a chave de domínio validando obrigatórios e normalizando algoritmo e uso.
    public JwkKey(String keyId,
                  String publicKeyPem,
                  String algorithm,
                  String use,
                  Instant createdAt,
                  boolean active) {

        this.keyId = Objects.requireNonNull(keyId, "keyId must not be null");
        this.publicKeyPem = Objects.requireNonNull(publicKeyPem, "publicKeyPem must not be null");
        this.algorithm = normalizeAlgorithm(algorithm);
        this.use = normalizeUse(use);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.active = active;
    }

    // Retorna o identificador (kid) da chave no JWKS.
    public String keyId() {
        return keyId;
    }

    // Retorna a chave pública em PEM (para persistência e publicação/transformação em JWK).
    public String publicKeyPem() {
        return publicKeyPem;
    }

    // Retorna o algoritmo associado à chave (ex.: RS256) já normalizado.
    public String algorithm() {
        return algorithm;
    }

    // Retorna o uso da chave ("sig" ou "enc") já normalizado.
    public String use() {
        return use;
    }

    // Retorna o instante de criação da chave para auditoria e rotação.
    public Instant createdAt() {
        return createdAt;
    }

    // Indica se a chave está ativa para uso/publicação no JWKS.
    public boolean active() {
        return active;
    }

    // Indica se a chave é destinada a assinatura de tokens ("sig").
    public boolean isSigningKey() {
        return USE_SIGNING.equalsIgnoreCase(use);
    }

    // Indica se a chave é destinada a criptografia ("enc").
    public boolean isEncryptionKey() {
        return USE_ENCRYPTION.equalsIgnoreCase(use);
    }

    // Normaliza e valida o algoritmo (trim, não-vazio e upper-case por convenção).
    private String normalizeAlgorithm(String algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        String trimmed = algorithm.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("algorithm must not be blank");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    // Normaliza e valida o uso da chave, aplicando default seguro ("sig") quando ausente.
    private String normalizeUse(String use) {
        if (use == null || use.isBlank()) {
            return USE_SIGNING;
        }
        String normalized = use.trim().toLowerCase(Locale.ROOT);
        if (!USE_SIGNING.equals(normalized) && !USE_ENCRYPTION.equals(normalized)) {
            throw new IllegalArgumentException("Invalid JWK use: " + use + ". Expected 'sig' or 'enc'.");
        }
        return normalized;
    }

    // Compara chaves por keyId, assumindo unicidade do kid no contexto do JWKS.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JwkKey jwkKey)) return false;
        return Objects.equals(keyId, jwkKey.keyId);
    }

    // Gera hashCode consistente com equals, usando apenas keyId.
    @Override
    public int hashCode() {
        return Objects.hash(keyId);
    }

    // Fornece representação textual útil para logs/diagnóstico sem expor o PEM.
    @Override
    public String toString() {
        return "JwkKey{" +
                "keyId='" + keyId + '\'' +
                ", algorithm='" + algorithm + '\'' +
                ", use='" + use + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}