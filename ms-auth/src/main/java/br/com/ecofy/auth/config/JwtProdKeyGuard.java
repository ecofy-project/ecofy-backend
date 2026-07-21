package br.com.ecofy.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// Valida a segurança da configuração de chaves no ambiente de produção.
@Configuration
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class JwtProdKeyGuard {

    private final KeysProperties keysProperties;

    // Interrompe a inicialização quando a configuração de chaves é insegura.
    @PostConstruct
    void verifyProductionKeysAreConfigured() {
        if (keysProperties.isAllowGeneratedKey()) {
            throw new IllegalStateException(
                    "Profile 'prod' não permite geração de chave em memória. "
                            + "Defina ecofy.auth.keys.allow-generated-key=false e forneça a chave por secret externo."
            );
        }

        if (isBlank(keysProperties.getActiveKid())) {
            throw new IllegalStateException(
                    "Profile 'prod' requer ecofy.auth.keys.active-kid (o kid vai no header do JWT e no JWKS)."
            );
        }

        boolean hasInline = !isBlank(
                keysProperties.getActivePrivateKey()
        );
        boolean hasLocation = !isBlank(
                keysProperties.getActivePrivateKeyLocation()
        );

        if (!hasInline && !hasLocation) {
            throw new IllegalStateException(
                    "Profile 'prod' requer a chave de assinatura por fonte externa "
                            + "(ecofy.auth.keys.active-private-key ou active-private-key-location)."
            );
        }

        if (hasLocation
                && isClasspath(
                keysProperties.getActivePrivateKeyLocation()
        )) {
            throw new IllegalStateException(
                    "Profile 'prod' não pode usar chave privada empacotada no classpath. "
                            + "Use file:/ (secret montado) ou injete o PEM por variável de ambiente."
            );
        }

        log.info(
                "[JwtProdKeyGuard] -> Configuração de chaves validada para prod activeKid={}",
                keysProperties.getActiveKid()
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isClasspath(String value) {
        return value.trim()
                .toLowerCase()
                .startsWith("classpath:");
    }
}
