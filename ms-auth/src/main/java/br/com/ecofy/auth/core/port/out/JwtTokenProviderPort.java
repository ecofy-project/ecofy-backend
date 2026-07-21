package br.com.ecofy.auth.core.port.out;

import br.com.ecofy.auth.core.domain.JwtToken;

import java.util.Map;

public interface JwtTokenProviderPort {

    JwtToken generateAccessToken(String subject, Map<String, Object> claims, long ttlSeconds);

    JwtToken generateRefreshToken(String subject, Map<String, Object> claims, long ttlSeconds);

    boolean isValid(String token);

    Map<String, Object> parseClaims(String token);

    // Valida assinatura, expiração e issuer do token, retornando as claims apenas se confiável.
    Map<String, Object> verifyAndParseClaims(String token);

}