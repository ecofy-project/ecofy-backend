package br.com.ecofy.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Configura os identificadores, as chaves e os tempos utilizados nos tokens JWT.
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String issuer;

    private String audience;

    private String keyId;

    private String privateKeyLocation;

    private String publicKeyLocation;

    private final Long accessTokenTtlSeconds = 900L;

    private final Long refreshTokenTtlSeconds =
            60L * 60L * 24L * 30L;

    private final Long clockSkewSeconds = 60L;

    public String getPublicKeyLocation() {
        return publicKeyLocation;
    }

    public void setPublicKeyLocation(String publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(
            @DefaultValue("https://auth.ecofy.com") String issuer
    ) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(
            @DefaultValue("ecofy-api") String audience
    ) {
        this.audience = audience;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    public void setPrivateKeyLocation(String privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
    }

    public Long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public Long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    public Long getClockSkewSeconds() {
        return clockSkewSeconds;
    }
}
