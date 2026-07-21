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

// Constrói o documento JWKS a partir das chaves públicas de assinatura.
@Slf4j
@Service
public class JwksService implements GetJwksUseCase {

    private final PublicSigningKeyProviderPort publicSigningKeyProviderPort;

    public JwksService(
            PublicSigningKeyProviderPort publicSigningKeyProviderPort
    ) {
        this.publicSigningKeyProviderPort = Objects.requireNonNull(
                publicSigningKeyProviderPort,
                "publicSigningKeyProviderPort must not be null"
        );
    }

    // Valida as chaves disponíveis e retorna o documento JWKS correspondente.
    @Override
    public Map<String, Object> getJwks() {
        log.debug(
                "[JwksService] - [getJwks] -> Derivando JWKS da chave pública de assinatura…"
        );

        List<Map<String, Object>> jwkList =
                publicSigningKeyProviderPort.currentPublicJwks();

        if (jwkList == null || jwkList.isEmpty()) {
            log.warn(
                    "[JwksService] - [getJwks] -> Nenhuma chave pública de assinatura disponível."
            );

            throw new AuthException(
                    AuthErrorCode.JWKS_NOT_AVAILABLE,
                    "No active signing keys available"
            );
        }

        Map<String, Object> response =
                Map.of("keys", jwkList);

        log.debug(
                "[JwksService] - [getJwks] -> JWKS gerado com sucesso totalKeys={}",
                jwkList.size()
        );

        return response;
    }
}
