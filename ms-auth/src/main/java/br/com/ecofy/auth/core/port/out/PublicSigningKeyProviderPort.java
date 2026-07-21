package br.com.ecofy.auth.core.port.out;

import java.util.List;
import java.util.Map;

// Fornece o JWK set público derivado da chave que realmente assina os tokens emitidos.
public interface PublicSigningKeyProviderPort {

    // Recupera as entradas JWK públicas que compõem o documento do JWKS.
    List<Map<String, Object>> currentPublicJwks();
}
