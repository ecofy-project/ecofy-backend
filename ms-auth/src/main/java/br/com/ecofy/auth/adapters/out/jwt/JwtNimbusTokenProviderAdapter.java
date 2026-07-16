package br.com.ecofy.auth.adapters.out.jwt;

import br.com.ecofy.auth.config.JwtProperties;
import br.com.ecofy.auth.core.domain.JwtToken;
import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.port.out.JwtTokenProviderPort;
import br.com.ecofy.auth.core.port.out.PublicSigningKeyProviderPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Service
@Slf4j
public class JwtNimbusTokenProviderAdapter implements JwtTokenProviderPort, PublicSigningKeyProviderPort {

    private final JwtProperties jwtProperties;
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final JWSSigner signer;
    private final JWSHeader jwsHeader;

    // Decoder que valida ASSINATURA + expiração (+ issuer) usando a chave pública local.
    private final NimbusJwtDecoder verifyingDecoder;

    // Usado apenas em DEV para guardar o par gerado em memória
    private RSAPublicKey devPublicKey;

    // Carrega as chaves/configurações e prepara o header/signer para assinar JWTs via Nimbus.
    public JwtNimbusTokenProviderAdapter(JwtProperties jwtProperties, ResourceLoader resourceLoader) {
        this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties must not be null");

        this.privateKey = loadPrivateKey(resourceLoader, jwtProperties.getPrivateKeyLocation());
        this.publicKey = loadPublicKey(resourceLoader, jwtProperties.getPublicKeyLocation());

        this.signer = createSigner(privateKey);

        this.jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(jwtProperties.getKeyId())
                .type(JOSEObjectType.JWT)
                .build();

        this.verifyingDecoder = buildVerifyingDecoder(this.publicKey, jwtProperties);

        log.info(
                "[JwtNimbusTokenProviderAdapter] - [constructor] -> Chaves JWT carregadas com sucesso keyId={}",
                jwtProperties.getKeyId()
        );
    }

    // Constrói um decoder que valida assinatura RSA (RS256), expiração e issuer (quando configurado).
    private NimbusJwtDecoder buildVerifyingDecoder(RSAPublicKey publicKey, JwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        if (props.getIssuer() != null && !props.getIssuer().isBlank()) {
            validators.add(new JwtIssuerValidator(props.getIssuer()));
        }
        validators.add(JwtValidators.createDefault()); // timestamps (exp/nbf) + skew padrão
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));

        return decoder;
    }

    // Gera um Access Token (JWT) com subject/claims informados e tempo de vida (TTL) configurado.
    @Override
    public JwtToken generateAccessToken(String subject, Map<String, Object> claims, long ttlSeconds) {
        return generateToken(subject, claims, ttlSeconds, TokenType.ACCESS);
    }

    // Gera um Refresh Token (JWT) com subject/claims informados e tempo de vida (TTL) configurado.
    @Override
    public JwtToken generateRefreshToken(String subject, Map<String, Object> claims, long ttlSeconds) {
        return generateToken(subject, claims, ttlSeconds, TokenType.REFRESH);
    }

    // Gera um token de verificação de e-mail (JWT) adicionando um claim de propósito e usando TokenType.VERIFICATION.
    public JwtToken generateVerificationToken(String subject, Map<String, Object> claims, long ttlSeconds) {

        log.debug(
                "[JwtNimbusTokenProviderAdapter] - [generateVerificationToken] -> Gerando token de verificação subject={} ttlSeconds={}",
                subject,
                ttlSeconds
        );

        if (claims != null) {
            claims.putIfAbsent("purpose", "EMAIL_VERIFICATION");
        }

        return generateToken(subject, claims, ttlSeconds, TokenType.VERIFICATION);
    }

    // Gera um token de reset de senha (JWT) adicionando um claim de propósito e usando TokenType.PASSWORD_RESET.
    public JwtToken generatePasswordResetToken(String subject, Map<String, Object> claims, long ttlSeconds) {

        log.debug(
                "[JwtNimbusTokenProviderAdapter] - [generatePasswordResetToken] -> Gerando token de reset de senha subject={} ttlSeconds={}",
                subject,
                ttlSeconds
        );

        if (claims != null) {
            claims.putIfAbsent("purpose", "PASSWORD_RESET");
        }

        return generateToken(subject, claims, ttlSeconds, TokenType.PASSWORD_RESET);
    }

    // Centraliza a geração do JWT (claims padrão + claims extras) e assina com RSA, retornando JwtToken serializado.
    private JwtToken generateToken(
            String subject,
            Map<String, Object> claims,
            long ttlSeconds,
            TokenType type
    ) {

        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(type, "type must not be null");

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        log.debug(
                "[JwtNimbusTokenProviderAdapter] - [generateToken] -> Gerando token type={} subject={} ttlSeconds={}",
                type,
                subject,
                ttlSeconds
        );

        var builder = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .audience(jwtProperties.getAudience())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .notBeforeTime(Date.from(now.minusSeconds(jwtProperties.getClockSkewSeconds())))
                .claim("typ", type.name());

        if (claims != null) {
            claims.forEach(builder::claim);
        }

        SignedJWT signedJWT = new SignedJWT(jwsHeader, builder.build());
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            log.error(
                    "[JwtNimbusTokenProviderAdapter] - [generateToken] -> Erro ao assinar JWT subject={} type={} error={}",
                    subject,
                    type,
                    e.getMessage(),
                    e
            );
            throw new IllegalStateException("Error signing JWT", e);
        }

        String serialized = signedJWT.serialize();

        log.debug(
                "[JwtNimbusTokenProviderAdapter] - [generateToken] -> Token gerado com sucesso type={} subject={}",
                type,
                subject
        );

        return new JwtToken(serialized, exp, type);
    }

    // Valida se o token é parseável e se ainda não expirou (a assinatura é validada pelo Resource Server).
    @Override
    public boolean isValid(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            var claimsSet = jwt.getJWTClaimsSet();

            Date exp = claimsSet.getExpirationTime();
            TokenType type = resolveTokenType(claimsSet);

            boolean valid = exp != null && exp.after(new Date());

            log.debug(
                    "[JwtNimbusTokenProviderAdapter] - [isValid] -> Validação de expiração valid={} exp={} type={}",
                    valid,
                    exp,
                    type
            );

            // Assinatura é validada no Resource Server (JwtDecoder).
            return valid;
        } catch (Exception e) {
            log.warn(
                    "[JwtNimbusTokenProviderAdapter] - [isValid] -> Token inválido error={}",
                    e.getMessage()
            );
            return false;
        }
    }

    // Faz o parsing do JWT e retorna o mapa de claims (lança exceção se o token for inválido).
    @Override
    public Map<String, Object> parseClaims(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            var claimsSet = jwt.getJWTClaimsSet();
            var claims = claimsSet.getClaims();

            TokenType type = resolveTokenType(claimsSet);

            log.debug(
                    "[JwtNimbusTokenProviderAdapter] - [parseClaims] -> Claims parseadas com sucesso subject={} type={}",
                    claimsSet.getSubject(),
                    type
            );

            return claims;
        } catch (ParseException e) {
            log.error(
                    "[JwtNimbusTokenProviderAdapter] - [parseClaims] -> Token inválido error={}",
                    e.getMessage(),
                    e
            );
            throw new IllegalArgumentException("Invalid JWT", e);
        }
    }

    // Valida ASSINATURA (RSA) + expiração (+ issuer) e retorna as claims apenas se o token for confiável.
    @Override
    public Map<String, Object> verifyAndParseClaims(String token) {
        Objects.requireNonNull(token, "token must not be null");
        try {
            Jwt jwt = verifyingDecoder.decode(token);

            log.debug(
                    "[JwtNimbusTokenProviderAdapter] - [verifyAndParseClaims] -> Assinatura válida subject={}",
                    jwt.getSubject()
            );

            return new HashMap<>(jwt.getClaims());
        } catch (JwtException e) {
            log.warn(
                    "[JwtNimbusTokenProviderAdapter] - [verifyAndParseClaims] -> Token rejeitado na validação de assinatura/expiração error={}",
                    e.getMessage()
            );
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    // Retorna o JWK público (kid, kty, alg, use, n, e) derivado da chave real de assinatura, para o JWKS.
    @Override
    public List<Map<String, Object>> currentPublicJwks() {
        Map<String, Object> jwk = toRsaJwk().toPublicJWK().toJSONObject();
        log.debug(
                "[JwtNimbusTokenProviderAdapter] - [currentPublicJwks] -> JWK público derivado da chave de assinatura keyId={}",
                jwtProperties.getKeyId()
        );
        return List.of(jwk);
    }

    // Resolve o TokenType a partir do claim "typ", tolerando ausência ou valores desconhecidos.
    private TokenType resolveTokenType(JWTClaimsSet claimsSet) {
        Object raw = claimsSet.getClaim("typ");
        if (raw == null) {
            return null;
        }

        try {
            return TokenType.valueOf(raw.toString());
        } catch (IllegalArgumentException ex) {
            log.warn(
                    "[JwtNimbusTokenProviderAdapter] - [resolveTokenType] -> Valor de typ desconhecido typ={}",
                    raw
            );
            return null;
        }
    }

    // Cria o assinador RSA (RS256) usado para assinar os tokens gerados.
    private JWSSigner createSigner(RSAPrivateKey privateKey) {
        log.debug("[JwtNimbusTokenProviderAdapter] - [createSigner] -> Criando RSASSASigner");
        return new RSASSASigner(privateKey);
    }

    // Em DEV, gera um par RSA em memória e retorna a chave privada, guardando a pública para reutilização.
    private RSAPrivateKey loadPrivateKey(ResourceLoader resourceLoader, String location) {
        Objects.requireNonNull(location, "private key location must not be null");

        try {
            log.warn(
                    "[JwtNimbusTokenProviderAdapter] - [loadPrivateKey] -> MODO DEV: gerando chave RSA em memória (sem arquivo PEM)"
            );

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.devPublicKey = (RSAPublicKey) keyPair.getPublic();

            log.debug(
                    "[JwtNimbusTokenProviderAdapter] - [loadPrivateKey] -> Chave privada DEV gerada em memória com sucesso"
            );
            return privateKey;

        } catch (Exception e) {
            log.error(
                    "[JwtNimbusTokenProviderAdapter] - [loadPrivateKey] -> Falha ao gerar chave privada DEV error={}",
                    e.getMessage(),
                    e
            );
            throw new IllegalStateException("Could not generate in-memory DEV private key", e);
        }

        /*
         * (PROD) Carrega a private key PEM a partir do ResourceLoader (código mantido comentado).
         */
    }

    // Em DEV, reutiliza a chave pública gerada junto da privada (ou gera outra em memória se necessário).
    private RSAPublicKey loadPublicKey(ResourceLoader resourceLoader, String location) {
        Objects.requireNonNull(location, "public key location must not be null");

        if (this.devPublicKey != null) {
            log.debug(
                    "[JwtNimbusTokenProviderAdapter] - [loadPublicKey] -> MODO DEV: retornando chave pública em memória"
            );
            return this.devPublicKey;
        }

        try {
            log.warn(
                    "[JwtNimbusTokenProviderAdapter] - [loadPublicKey] -> MODO DEV: public key ainda não gerada, criando novo par RSA em memória"
            );

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            this.devPublicKey = publicKey;

            log.debug(
                    "[JwtNimbusTokenProviderAdapter] - [loadPublicKey] -> Chave pública DEV gerada em memória com sucesso"
            );
            return publicKey;

        } catch (Exception e) {
            log.error(
                    "[JwtNimbusTokenProviderAdapter] - [loadPublicKey] -> Falha ao gerar chave pública DEV error={}",
                    e.getMessage(),
                    e
            );
            throw new IllegalStateException("Could not generate in-memory DEV public key", e);
        }

        /*
         * (PROD) Carrega a public key PEM a partir do ResourceLoader (código mantido comentado).
         */
    }

    // Converte a chave pública carregada em um JWK (RSA) para publicação no endpoint de JWKS.
    public RSAKey toRsaJwk() {
        log.debug(
                "[JwtNimbusTokenProviderAdapter] - [toRsaJwk] -> Gerando JWK keyId={}",
                jwtProperties.getKeyId()
        );

        return new RSAKey.Builder(publicKey)
                .keyID(jwtProperties.getKeyId())
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }
}