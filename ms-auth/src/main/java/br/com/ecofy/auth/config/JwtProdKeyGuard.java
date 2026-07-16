package br.com.ecofy.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Guarda de segurança para o profile prod.
 *
 * Em dev/test a geração de chave RSA em memória é aceitável (documentada) — os
 * tokens não precisam sobreviver a restart. Em produção, porém, chaves geradas
 * em memória são inaceitáveis (tokens deixam de ser verificáveis após restart e
 * não há rotação/controle da chave privada).
 *
 * Este guard FALHA O STARTUP em prod caso as localizações das chaves não estejam
 * configuradas por fonte externa (variável de ambiente / secret / caminho de
 * arquivo). Assim, o serviço nunca sobe em produção dependendo apenas da chave
 * gerada em memória.
 *
 * Limitação conhecida (próximo passo): o carregamento efetivo do PEM externo e a
 * integração com secret manager ainda não estão implementados — ver README/relatório.
 */
@Configuration
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class JwtProdKeyGuard {

    private final JwtProperties jwtProperties;

    @PostConstruct
    void verifyProductionKeysAreConfigured() {
        String privateLocation = jwtProperties.getPrivateKeyLocation();
        String publicLocation = jwtProperties.getPublicKeyLocation();

        if (isBlank(privateLocation) || isBlank(publicLocation)) {
            throw new IllegalStateException(
                    "Profile 'prod' requer chaves JWT configuradas por fonte externa "
                            + "(security.jwt.private-key-location e security.jwt.public-key-location). "
                            + "Geração de chave em memória não é permitida em produção."
            );
        }

        if (isClasspath(privateLocation)) {
            throw new IllegalStateException(
                    "Profile 'prod' não pode usar chave privada empacotada no classpath ("
                            + privateLocation + "). Use file:/ ou um secret externo."
            );
        }

        log.info(
                "[JwtProdKeyGuard] -> Localizações de chave JWT configuradas para prod (privada e pública externas)."
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isClasspath(String value) {
        return value != null && value.trim().toLowerCase().startsWith("classpath:");
    }
}
