package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.JwkKey;
import br.com.ecofy.auth.core.port.in.GetJwksUseCase;
import br.com.ecofy.auth.core.port.out.JwksRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Serviço responsável por montar o JWKS (JSON Web Key Set) para exposição no endpoint /.well-known/jwks.json.
@Slf4j
@Service
public class JwksService implements GetJwksUseCase {

    private final JwksRepositoryPort jwksRepositoryPort;

    // Injeta o repositório de JWKS e garante que ele não seja nulo para consulta das chaves ativas.
    public JwksService(JwksRepositoryPort jwksRepositoryPort) {
        this.jwksRepositoryPort =
                Objects.requireNonNull(jwksRepositoryPort, "jwksRepositoryPort must not be null");
    }

    // Carrega as chaves de assinatura ativas e retorna um JWKS no formato {"keys": [...]} ou lança erro se não houver chaves.
    @Override
    public Map<String, Object> getJwks() {
        log.debug("[JwksService] - [getJwks] -> Buscando chaves ativas…");

        List<JwkKey> keys = jwksRepositoryPort.findActiveSigningKeys();

        if (keys.isEmpty()) {
            log.warn("[JwksService] - [getJwks] -> Nenhuma JWK ativa encontrada.");
            throw new AuthException(
                    AuthErrorCode.JWKS_NOT_AVAILABLE,
                    "No active signing keys available"
            );
        } else {
            log.debug(
                    "[JwksService] - [getJwks] -> {} chave(s) ativa(s) encontrada(s).",
                    keys.size()
            );
        }

        List<Map<String, Object>> jwkList = keys.stream()
                .map(this::convertToJwkEntry)
                .toList();

        Map<String, Object> response = Map.of("keys", jwkList);

        log.debug(
                "[JwksService] - [getJwks] -> JWKS gerado com sucesso totalKeys={}",
                jwkList.size()
        );

        return response;
    }

    // Converte uma chave de domínio (JwkKey) em um entry de JWK contendo os metadados públicos do JWKS.
    private Map<String, Object> convertToJwkEntry(JwkKey key) {
        Map<String, Object> m = new LinkedHashMap<>();

        m.put("kid", key.keyId());
        m.put("alg", key.algorithm());
        m.put("use", key.use());
        m.put("kty", "RSA");

        log.debug(
                "[JwksService] - [convertToJwkEntry] -> Convertendo keyId={} alg={} use={}",
                key.keyId(), key.algorithm(), key.use()
        );

        return m;
    }
}
