package br.com.ecofy.auth.core.port.out;

import java.util.List;
import java.util.Map;

/**
 * Fornece o conjunto de chaves públicas de assinatura (JWK set) DERIVADAS da
 * chave realmente usada para assinar os tokens.
 *
 * Isso garante que o JWKS publicado seja consistente com a assinatura dos JWTs
 * emitidos (mesmo kid, mesmo material público n/e), independentemente do profile
 * (dev/test/prod), permitindo que Resource Servers validem os tokens.
 */
public interface PublicSigningKeyProviderPort {

    /**
     * Retorna a lista de entradas JWK públicas (cada uma um mapa com, no mínimo,
     * kty, kid, alg, use, n, e) para compor o documento {"keys": [...]}.
     */
    List<Map<String, Object>> currentPublicJwks();
}
