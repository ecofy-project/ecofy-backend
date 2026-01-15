package br.com.ecofy.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    // Issuer padrão (claim "iss") aplicado a todos os tokens emitidos.
    private String issuer;

    // Audience padrão (claim "aud") usada para validar consumidores do token.
    private String audience;

    // Identificador da chave no JWKS (header "kid") para seleção/rotação de chaves.
    private String keyId;

    // Localização do arquivo PEM da chave privada RSA (classpath: ou file:), usada para assinar tokens.
    private String privateKeyLocation;

    // Localização do arquivo PEM da chave pública RSA (classpath: ou file:), usada para validação/publicação via JWKS.
    private String publicKeyLocation;

    // Tempo de vida do access token em segundos (default 15 minutos).
    private final Long accessTokenTtlSeconds = 900L;

    // Tempo de vida do refresh token em segundos (default 30 dias).
    private final Long refreshTokenTtlSeconds = 60L * 60L * 24L * 30L;

    // Clock skew permitido na validação de tempo do token (em segundos).
    private final Long clockSkewSeconds = 60L;

    // Retorna a localização do PEM da chave pública RSA.
    public String getPublicKeyLocation() {
        return publicKeyLocation;
    }

    // Define a localização do PEM da chave pública RSA.
    public void setPublicKeyLocation(String publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }

    // Retorna o issuer configurado para os tokens (claim "iss").
    public String getIssuer() {
        return issuer;
    }

    // Define o issuer dos tokens (claim "iss"), aplicando valor padrão quando não configurado.
    public void setIssuer(@DefaultValue("https://auth.ecofy.com") String issuer) {
        this.issuer = issuer;
    }

    // Retorna a audience configurada para os tokens (claim "aud").
    public String getAudience() {
        return audience;
    }

    // Define a audience dos tokens (claim "aud"), aplicando valor padrão quando não configurado.
    public void setAudience(@DefaultValue("ecofy-api") String audience) {
        this.audience = audience;
    }

    // Retorna o keyId (kid) da chave usada na assinatura/publicação do JWKS.
    public String getKeyId() {
        return keyId;
    }

    // Define o keyId (kid) da chave usada na assinatura/publicação do JWKS.
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    // Retorna a localização do PEM da chave privada RSA.
    public String getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    // Define a localização do PEM da chave privada RSA.
    public void setPrivateKeyLocation(String privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
    }

    // Retorna o TTL do access token (em segundos).
    public Long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    // Retorna o TTL do refresh token (em segundos).
    public Long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    // Retorna o clock skew permitido na validação do token (em segundos).
    public Long getClockSkewSeconds() {
        return clockSkewSeconds;
    }

}
