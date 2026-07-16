package br.com.ecofy.auth.core.port.out;

import br.com.ecofy.auth.core.domain.JwtToken;

import java.util.Map;

public interface JwtTokenProviderPort {

    JwtToken generateAccessToken(String subject, Map<String, Object> claims, long ttlSeconds);

    JwtToken generateRefreshToken(String subject, Map<String, Object> claims, long ttlSeconds);

    boolean isValid(String token);

    Map<String, Object> parseClaims(String token);

    /**
     * Verifica a ASSINATURA (RSA/JWKS), a expiração e (quando configurado) o issuer
     * do token, retornando as claims apenas se o token for realmente confiável.
     * Diferente de {@link #isValid(String)}/{@link #parseClaims(String)}, que apenas
     * fazem parsing/checagem de expiração sem validar a assinatura.
     *
     * @throws IllegalArgumentException se a assinatura for inválida, o token estiver
     *                                  expirado ou for malformado.
     */
    Map<String, Object> verifyAndParseClaims(String token);

}