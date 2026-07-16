package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.port.in.GetJwksUseCase;
import br.com.ecofy.auth.core.port.out.PublicSigningKeyProviderPort;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Serviço responsável por montar o JWKS (JSON Web Key Set) para exposição no endpoint /.well-known/jwks.json.
//
// O JWKS é derivado da CHAVE PÚBLICA REAL de assinatura (mesmo material n/e e kid
// usados para assinar os JWTs), garantindo que Resource Servers consigam validar
// os tokens emitidos pelo ms-auth em qualquer profile (dev/test/prod).
@Slf4j
@Service
public class JwksService implements GetJwksUseCase {

    private final PublicSigningKeyProviderPort publicSigningKeyProviderPort;

    // Injeta o provedor da chave pública de assinatura e garante que ele não seja nulo.
    public JwksService(PublicSigningKeyProviderPort publicSigningKeyProviderPort) {
        this.publicSigningKeyProviderPort =
                Objects.requireNonNull(publicSigningKeyProviderPort, "publicSigningKeyProviderPort must not be null");
    }

    // Carrega as chaves públicas de assinatura e retorna um JWKS no formato {"keys": [...]} ou lança erro se não houver chaves.
    @Override
    public Map<String, Object> getJwks() {
        log.debug("[JwksService] - [getJwks] -> Derivando JWKS da chave pública de assinatura…");

        List<Map<String, Object>> jwkList = publicSigningKeyProviderPort.currentPublicJwks();

        if (jwkList == null || jwkList.isEmpty()) {
            log.warn("[JwksService] - [getJwks] -> Nenhuma chave pública de assinatura disponível.");
            throw new AuthException(
                    AuthErrorCode.JWKS_NOT_AVAILABLE,
                    "No active signing keys available"
            );
        }

        Map<String, Object> response = Map.of("keys", jwkList);

        log.debug(
                "[JwksService] - [getJwks] -> JWKS gerado com sucesso totalKeys={}",
                jwkList.size()
        );

        return response;
    }
}
