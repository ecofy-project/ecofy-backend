package br.com.ecofy.auth.adapters.out.jwt;

import br.com.ecofy.auth.config.JwtProperties;
import br.com.ecofy.auth.core.domain.JwtToken;
import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.domain.keys.ActiveSigningKey;
import br.com.ecofy.auth.core.domain.keys.VerificationKey;
import br.com.ecofy.auth.core.port.out.JwtTokenProviderPort;
import br.com.ecofy.auth.core.port.out.PublicSigningKeyProviderPort;
import br.com.ecofy.auth.core.port.out.SigningKeyProviderPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

// Centraliza a emissão, a verificação e a exposição das chaves públicas dos tokens JWT.
@Component
@Slf4j
public class JwtNimbusTokenProviderAdapter
        implements JwtTokenProviderPort, PublicSigningKeyProviderPort {

    private static final JWSAlgorithm ALG = JWSAlgorithm.RS256;

    private final JwtProperties jwtProperties;
    private final SigningKeyProviderPort signingKeyProvider;
    private final JWSSigner signer;
    private final JWSHeader jwsHeader;
    private final NimbusJwtDecoder verifyingDecoder;

    public JwtNimbusTokenProviderAdapter(
            JwtProperties jwtProperties,
            SigningKeyProviderPort signingKeyProvider
    ) {
        this.jwtProperties = Objects.requireNonNull(
                jwtProperties,
                "jwtProperties must not be null"
        );
        this.signingKeyProvider = Objects.requireNonNull(
                signingKeyProvider,
                "signingKeyProvider must not be null"
        );

        ActiveSigningKey active = signingKeyProvider.activeKey();

        this.signer = new RSASSASigner(active.privateKey());
        this.jwsHeader = new JWSHeader.Builder(ALG)
                .keyID(active.kid())
                .type(JOSEObjectType.JWT)
                .build();

        this.verifyingDecoder = buildVerifyingDecoder();

        log.info(
                "[JwtNimbusTokenProviderAdapter] - [constructor] -> Provider de chaves ativo activeKid={} verificationKeys={}",
                active.kid(),
                signingKeyProvider.verificationKeys().size()
        );
    }

    // Configura o decoder responsável pela validação criptográfica dos tokens.
    private NimbusJwtDecoder buildVerifyingDecoder() {
        JWKSet jwkSet = new JWKSet(
                signingKeyProvider.verificationKeys().stream()
                        .map(JwtNimbusTokenProviderAdapter::toRsaKey)
                        .map(com.nimbusds.jose.jwk.JWK.class::cast)
                        .toList()
        );

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

        JWSKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(ALG, jwkSource);

        DefaultJWTProcessor<SecurityContext> processor =
                new DefaultJWTProcessor<>();

        processor.setJWSKeySelector(keySelector);
        processor.setJWTClaimsSetVerifier((claims, context) -> { });

        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();

        if (jwtProperties.getIssuer() != null
                && !jwtProperties.getIssuer().isBlank()) {
            validators.add(
                    new JwtIssuerValidator(jwtProperties.getIssuer())
            );
        }

        validators.add(JwtValidators.createDefault());

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(validators)
        );

        return decoder;
    }

    @Override
    public JwtToken generateAccessToken(
            String subject,
            Map<String, Object> claims,
            long ttlSeconds
    ) {
        return generateToken(
                subject,
                claims,
                ttlSeconds,
                TokenType.ACCESS
        );
    }

    @Override
    public JwtToken generateRefreshToken(
            String subject,
            Map<String, Object> claims,
            long ttlSeconds
    ) {
        return generateToken(
                subject,
                claims,
                ttlSeconds,
                TokenType.REFRESH
        );
    }

    // Gera um token com o propósito de confirmação de e-mail.
    public JwtToken generateVerificationToken(
            String subject,
            Map<String, Object> claims,
            long ttlSeconds
    ) {
        if (claims != null) {
            claims.putIfAbsent(
                    "purpose",
                    "EMAIL_VERIFICATION"
            );
        }

        return generateToken(
                subject,
                claims,
                ttlSeconds,
                TokenType.VERIFICATION
        );
    }

    // Gera um token com o propósito de redefinição de senha.
    public JwtToken generatePasswordResetToken(
            String subject,
            Map<String, Object> claims,
            long ttlSeconds
    ) {
        if (claims != null) {
            claims.putIfAbsent(
                    "purpose",
                    "PASSWORD_RESET"
            );
        }

        return generateToken(
                subject,
                claims,
                ttlSeconds,
                TokenType.PASSWORD_RESET
        );
    }

    // Gera e assina um token com as claims e o período de validade informados.
    private JwtToken generateToken(
            String subject,
            Map<String, Object> claims,
            long ttlSeconds,
            TokenType type
    ) {
        Objects.requireNonNull(
                subject,
                "subject must not be null"
        );
        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        var builder = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .audience(jwtProperties.getAudience())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .notBeforeTime(
                        Date.from(
                                now.minusSeconds(
                                        jwtProperties.getClockSkewSeconds()
                                )
                        )
                )
                .claim("typ", type.name());

        if (claims != null) {
            claims.forEach(builder::claim);
        }

        builder.jwtID(UUID.randomUUID().toString());

        if (type == TokenType.ACCESS) {
            builder.claim("authUserId", subject);
        }

        SignedJWT signedJWT = new SignedJWT(
                jwsHeader,
                builder.build()
        );

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

            throw new IllegalStateException(
                    "Error signing JWT",
                    e
            );
        }

        return new JwtToken(
                signedJWT.serialize(),
                exp,
                type
        );
    }

    // Valida se o token pode ser interpretado e ainda não expirou.
    @Override
    public boolean isValid(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            Date exp = jwt.getJWTClaimsSet().getExpirationTime();

            return exp != null && exp.after(new Date());
        } catch (Exception e) {
            log.warn(
                    "[JwtNimbusTokenProviderAdapter] - [isValid] -> Token inválido error={}",
                    e.getMessage()
            );

            return false;
        }
    }

    // Extrai as claims sem executar validação criptográfica.
    @Override
    public Map<String, Object> parseClaims(String token) {
        try {
            return SignedJWT.parse(token)
                    .getJWTClaimsSet()
                    .getClaims();
        } catch (ParseException e) {
            log.error(
                    "[JwtNimbusTokenProviderAdapter] - [parseClaims] -> Token inválido error={}",
                    e.getMessage(),
                    e
            );

            throw new IllegalArgumentException(
                    "Invalid JWT",
                    e
            );
        }
    }

    // Valida o token e retorna as claims verificadas.
    @Override
    public Map<String, Object> verifyAndParseClaims(String token) {
        Objects.requireNonNull(
                token,
                "token must not be null"
        );

        try {
            Jwt jwt = verifyingDecoder.decode(token);

            return new HashMap<>(jwt.getClaims());
        } catch (JwtException e) {
            log.warn(
                    "[JwtNimbusTokenProviderAdapter] - [verifyAndParseClaims] -> Token rejeitado error={}",
                    e.getMessage()
            );

            throw new IllegalArgumentException(
                    "Invalid token",
                    e
            );
        }
    }

    // Converte as chaves públicas válidas para o formato JWKS.
    @Override
    public List<Map<String, Object>> currentPublicJwks() {
        return signingKeyProvider.verificationKeys().stream()
                .map(JwtNimbusTokenProviderAdapter::toRsaKey)
                .map(rsaKey -> rsaKey.toPublicJWK().toJSONObject())
                .toList();
    }

    public NimbusJwtDecoder jwtDecoder() {
        return verifyingDecoder;
    }

    public RSAKey toRsaJwk() {
        ActiveSigningKey active = signingKeyProvider.activeKey();

        return new RSAKey.Builder(active.publicKey())
                .keyID(active.kid())
                .algorithm(ALG)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    private static RSAKey toRsaKey(VerificationKey key) {
        return new RSAKey.Builder(key.publicKey())
                .keyID(key.kid())
                .algorithm(ALG)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }
}