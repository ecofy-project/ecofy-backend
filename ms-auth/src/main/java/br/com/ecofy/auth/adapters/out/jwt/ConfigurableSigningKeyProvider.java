package br.com.ecofy.auth.adapters.out.jwt;

import br.com.ecofy.auth.config.KeysProperties;
import br.com.ecofy.auth.core.domain.keys.ActiveSigningKey;
import br.com.ecofy.auth.core.domain.keys.SigningKeyMetadata;
import br.com.ecofy.auth.core.domain.keys.VerificationKey;
import br.com.ecofy.auth.core.port.out.SigningKeyProviderPort;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

// Resolve as chaves de assinatura e verificação a partir das configurações disponíveis.
@Component
@Slf4j
public class ConfigurableSigningKeyProvider implements SigningKeyProviderPort {

    private static final String RSA = "RSA";
    private static final int GENERATED_KEY_SIZE = 2048;

    private final ActiveSigningKey activeKey;
    private final List<VerificationKey> verificationKeys;
    private final Clock clock;

    public ConfigurableSigningKeyProvider(
            KeysProperties properties,
            ResourceLoader resourceLoader,
            Clock clock
    ) {
        this.clock = clock;
        validateAlgorithm(properties.getAlgorithm());

        Instant now = clock.instant();
        this.activeKey = resolveActiveKey(properties, resourceLoader, now);

        List<VerificationKey> keys = new ArrayList<>();
        keys.add(new VerificationKey(activeKey.metadata(), activeKey.publicKey()));
        keys.addAll(resolveRetiringKeys(properties, resourceLoader, now));
        this.verificationKeys = List.copyOf(keys);

        log.info(
                "[ConfigurableSigningKeyProvider] -> Chaves carregadas activeKid={} verificationKeys={}",
                activeKey.kid(),
                this.verificationKeys.size()
        );
    }

    @Override
    public ActiveSigningKey activeKey() {
        return activeKey;
    }

    // Filtra as chaves públicas disponíveis dentro da janela de verificação.
    @Override
    public List<VerificationKey> verificationKeys() {
        Instant now = clock.instant();

        return verificationKeys.stream()
                .filter(k -> k.metadata().isValidForVerificationAt(now))
                .toList();
    }

    // Resolve a chave ativa configurada ou gera uma chave temporária quando permitido.
    private ActiveSigningKey resolveActiveKey(
            KeysProperties properties,
            ResourceLoader resourceLoader,
            Instant now
    ) {
        String kid = properties.getActiveKid();

        if (kid == null || kid.isBlank()) {
            throw new IllegalStateException(
                    "ECO-20: 'ecofy.auth.keys.active-kid' é obrigatório (o kid vai no header do JWT e no JWKS)."
            );
        }

        String pem = readPem(
                properties.getActivePrivateKey(),
                properties.getActivePrivateKeyLocation(),
                resourceLoader,
                "chave privada ativa"
        );

        SigningKeyMetadata metadata = new SigningKeyMetadata(
                kid,
                properties.getAlgorithm(),
                SigningKeyMetadata.Status.ACTIVE,
                now,
                null,
                null
        );

        if (pem != null) {
            RSAPrivateKey privateKey = parsePrivateKey(pem);
            RSAPublicKey publicKey = derivePublicKey(privateKey);

            log.info(
                    "[ConfigurableSigningKeyProvider] -> Chave ativa carregada de secret externo kid={}",
                    kid
            );

            return new ActiveSigningKey(metadata, privateKey, publicKey);
        }

        if (!properties.isAllowGeneratedKey()) {
            throw new IllegalStateException(
                    "ECO-20: nenhuma chave de assinatura configurada e a geração em memória está desabilitada "
                            + "(ecofy.auth.keys.allow-generated-key=false). Configure 'active-private-key' ou "
                            + "'active-private-key-location' via secret externo."
            );
        }

        log.warn(
                "[ConfigurableSigningKeyProvider] -> MODO DEV: gerando par RSA EFÊMERO em memória (kid={}). "
                        + "Tokens deixam de ser verificáveis após restart. NUNCA use em produção.",
                kid
        );

        KeyPair pair = generateKeyPair();

        return new ActiveSigningKey(
                metadata,
                (RSAPrivateKey) pair.getPrivate(),
                (RSAPublicKey) pair.getPublic()
        );
    }

    // Resolve as chaves públicas mantidas durante o período de rotação.
    private List<VerificationKey> resolveRetiringKeys(
            KeysProperties properties,
            ResourceLoader resourceLoader,
            Instant now
    ) {
        List<VerificationKey> result = new ArrayList<>();
        Instant expiresAt = now.plus(properties.getRetentionWindow());

        for (KeysProperties.RetiringKey retiring : properties.getRetiring()) {
            if (retiring.getKid() == null || retiring.getKid().isBlank()) {
                throw new IllegalStateException(
                        "Retired signing key is missing required 'kid' for JWKS"
                );
            }

            String pem = readPem(
                    retiring.getPublicKey(),
                    retiring.getPublicKeyLocation(),
                    resourceLoader,
                    "chave pública aposentada " + retiring.getKid()
            );

            if (pem == null) {
                throw new IllegalStateException(
                        "ECO-20: chave aposentada kid="
                                + retiring.getKid()
                                + " sem material público configurado."
                );
            }

            SigningKeyMetadata metadata = new SigningKeyMetadata(
                    retiring.getKid(),
                    properties.getAlgorithm(),
                    SigningKeyMetadata.Status.RETIRING,
                    null,
                    now,
                    expiresAt
            );

            result.add(
                    new VerificationKey(metadata, parsePublicKey(pem))
            );
        }

        return result;
    }

    // Carrega o conteúdo PEM informado diretamente ou por recurso externo.
    private String readPem(
            String inline,
            String location,
            ResourceLoader resourceLoader,
            String description
    ) {
        if (inline != null && !inline.isBlank()) {
            return inline;
        }

        if (location == null || location.isBlank()) {
            return null;
        }

        Resource resource = resourceLoader.getResource(location);

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "ECO-20: "
                            + description
                            + " não encontrada na localização configurada."
            );
        }

        try (InputStream in = resource.getInputStream()) {
            return new String(
                    in.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read signing key material: " + description,
                    e
            );
        }
    }

    // Converte o conteúdo PEM em uma chave privada RSA.
    static RSAPrivateKey parsePrivateKey(String pem) {
        try {
            byte[] der = decodePem(pem, "PRIVATE KEY");
            KeyFactory factory = KeyFactory.getInstance(RSA);

            return (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(der)
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Invalid private key (expected PEM PKCS#8 RSA)",
                    e
            );
        }
    }

    // Converte o conteúdo PEM em uma chave pública RSA.
    static RSAPublicKey parsePublicKey(String pem) {
        try {
            byte[] der = decodePem(pem, "PUBLIC KEY");
            KeyFactory factory = KeyFactory.getInstance(RSA);

            return (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(der)
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Invalid public key (expected PEM X.509 RSA)",
                    e
            );
        }
    }

    // Decodifica o conteúdo Base64 delimitado pelo formato PEM.
    private static byte[] decodePem(String pem, String type) {
        String normalized = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("PEM vazio");
        }

        return Base64.getDecoder().decode(normalized);
    }

    // Deriva a chave pública correspondente a uma chave privada RSA.
    private static RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crt)) {
            throw new IllegalStateException(
                    "ECO-20: chave privada não permite derivar a pública (esperado RSA CRT/PKCS#8)."
            );
        }

        try {
            KeyFactory factory = KeyFactory.getInstance(RSA);
            BigInteger modulus = crt.getModulus();
            BigInteger publicExponent = crt.getPublicExponent();

            return (RSAPublicKey) factory.generatePublic(
                    new RSAPublicKeySpec(modulus, publicExponent)
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to derive public key from private key",
                    e
            );
        }
    }

    // Gera um par de chaves RSA para ambientes que permitem chaves temporárias.
    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
            generator.initialize(GENERATED_KEY_SIZE);

            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate in-memory RSA key pair",
                    e
            );
        }
    }

    // Valida se o algoritmo de assinatura está entre os valores permitidos.
    private static void validateAlgorithm(String algorithm) {
        if (!"RS256".equals(algorithm)) {
            throw new IllegalStateException(
                    "ECO-20: algoritmo de assinatura não permitido: "
                            + algorithm
                            + " (permitido: RS256)."
            );
        }
    }
}