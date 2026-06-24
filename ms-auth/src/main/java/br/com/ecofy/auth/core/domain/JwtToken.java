package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.TokenType;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

// Value object que encapsula um JWT emitido, com expiração e tipo, evitando uso indevido do token como String solta.
public final class JwtToken {

    // Valor serializado do JWT (header.payload.signature).
    private final String value;

    // Instante exato em que o token expira.
    private final Instant expiresAt;

    // Tipo do token: ACCESS / REFRESH.
    private final TokenType type;

    // Constrói o token validando/normalizando o valor, definindo expiração e tipo, e evitando token já expirado.
    public JwtToken(String value, Instant expiresAt, TokenType type) {
        this.value = normalizeToken(value);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");

        if (expiresAt.isBefore(Instant.now().minusSeconds(5))) {
            // Bloqueia a criação de tokens efetivamente expirados (tolerância pequena para clock skew).
            throw new IllegalArgumentException("expiresAt cannot be in the past");
        }
    }

    // Normaliza e valida o token (trim e não-vazio) antes de armazenar.
    private String normalizeToken(String raw) {
        Objects.requireNonNull(raw, "value must not be null");
        String token = raw.trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("JWT value must not be blank");
        }
        return token;
    }

    // Retorna o valor serializado do JWT (header.payload.signature).
    public String value() {
        return value;
    }

    // Retorna o instante exato em que o token expira.
    public Instant expiresAt() {
        return expiresAt;
    }

    // Retorna o tipo do token (ACCESS/REFRESH/etc.).
    public TokenType type() {
        return type;
    }

    // Indica se o token já expirou considerando o relógio atual.
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // Indica se o token ainda está ativo (não expirado).
    public boolean isActive() {
        return !isExpired();
    }

    // Calcula o tempo restante até a expiração do token.
    public Duration timeToExpire() {
        return Duration.between(Instant.now(), expiresAt);
    }

    // Indica se o token está prestes a expirar dado um limite (threshold).
    public boolean isAboutToExpire(Duration threshold) {
        Objects.requireNonNull(threshold, "threshold must not be null");
        return !isExpired() && timeToExpire().compareTo(threshold) <= 0;
    }

    // Compara tokens por valor serializado (identidade do JWT).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JwtToken that)) return false;
        return Objects.equals(value, that.value);
    }

    // Gera hashCode consistente com equals usando o valor do token.
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    // Retorna uma representação segura do token, mascarando o valor para evitar vazamento em logs.
    @Override
    public String toString() {
        String masked = value.length() > 12 ? value.substring(0, 12) + "..." : "***";
        return "JwtToken{" +
                "value='" + masked + '\'' +
                ", expiresAt=" + expiresAt +
                ", type=" + type +
                '}';
    }
}