package br.com.ecofy.auth.adapters.out.jwt;

import br.com.ecofy.auth.config.JwtProperties;
import br.com.ecofy.auth.core.domain.JwtToken;
import br.com.ecofy.auth.core.domain.enums.TokenType;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtNimbusTokenProviderAdapterTest {

    @Test
    @DisplayName("constructor: jwtProperties null -> NPE com mensagem")
    void constructor_jwtPropertiesNull_throwsNpe() {
        assertThatThrownBy(() -> new JwtNimbusTokenProviderAdapter(null, mock(ResourceLoader.class)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("jwtProperties must not be null");
    }

    @Test
    @DisplayName("constructor: privateKeyLocation null -> NPE com mensagem")
    void constructor_privateKeyLocationNull_throwsNpe() {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn(null);

        assertThatThrownBy(() -> new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("private key location must not be null");
    }

    @Test
    @DisplayName("constructor: publicKeyLocation null -> NPE com mensagem")
    void constructor_publicKeyLocationNull_throwsNpe() {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:priv");
        when(p.getPublicKeyLocation()).thenReturn(null);

        assertThatThrownBy(() -> new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("public key location must not be null");
    }

    @Test
    @DisplayName("generateAccessToken: inclui issuer/audience/sub/typ/iat/nbf/exp + claims custom")
    void generateAccessToken_success_withClaims() throws Exception {
        JwtNimbusTokenProviderAdapter a = adapter(propsForTokenFlow("kid-1", "iss", "aud", 5));

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");

        JwtToken t = a.generateAccessToken("sub-1", claims, 60);

        assertThat(t.type()).isEqualTo(TokenType.ACCESS);
        assertThat(t.value()).isNotBlank();
        assertThat(t.expiresAt()).isAfter(Instant.now());

        SignedJWT parsed = SignedJWT.parse(t.value());
        JWTClaimsSet cs = parsed.getJWTClaimsSet();

        assertThat(cs.getSubject()).isEqualTo("sub-1");
        assertThat(cs.getIssuer()).isEqualTo("iss");
        assertThat(cs.getAudience()).containsExactly("aud");
        assertThat(cs.getClaim("typ")).isEqualTo("ACCESS");
        assertThat(cs.getClaim("role")).isEqualTo("USER");

        assertThat(cs.getIssueTime()).isNotNull();
        assertThat(cs.getNotBeforeTime()).isNotNull();
        assertThat(cs.getExpirationTime()).isNotNull();
        assertThat(cs.getNotBeforeTime()).isBeforeOrEqualTo(cs.getIssueTime());
    }

    @Test
    @DisplayName("generateRefreshToken: claims null e typ=REFRESH")
    void generateRefreshToken_success_nullClaims() throws Exception {
        JwtNimbusTokenProviderAdapter a = adapter(propsForTokenFlow("kid-1", "iss", "aud", 0));

        JwtToken t = a.generateRefreshToken("sub-r", null, 60);

        SignedJWT parsed = SignedJWT.parse(t.value());
        assertThat(parsed.getJWTClaimsSet().getClaim("typ")).isEqualTo("REFRESH");
        assertThat(t.type()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    @DisplayName("generateVerificationToken: adiciona purpose se ausente (putIfAbsent) e typ=VERIFICATION")
    void generateVerificationToken_addsPurposeIfAbsent() throws Exception {
        JwtNimbusTokenProviderAdapter a = adapter(propsForTokenFlow("kid-1", "iss", "aud", 0));

        Map<String, Object> claims = new HashMap<>();
        claims.put("x", 1);

        JwtToken t = a.generateVerificationToken("sub-v", claims, 60);

        SignedJWT parsed = SignedJWT.parse(t.value());
        JWTClaimsSet cs = parsed.getJWTClaimsSet();

        assertThat(t.type()).isEqualTo(TokenType.VERIFICATION);
        assertThat(cs.getClaim("typ")).isEqualTo("VERIFICATION");
        assertThat(cs.getClaim("purpose")).isEqualTo("EMAIL_VERIFICATION");

        Object x = cs.getClaim("x");
        assertThat(x).isInstanceOfAny(Integer.class, Long.class);
        assertThat(((Number) x).longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("generateVerificationToken: mantém purpose existente")
    void generateVerificationToken_keepsExistingPurpose() throws Exception {
        JwtNimbusTokenProviderAdapter a = adapter(propsForTokenFlow("kid-1", "iss", "aud", 0));

        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "CUSTOM");

        JwtToken t = a.generateVerificationToken("sub-v2", claims, 60);

        SignedJWT parsed = SignedJWT.parse(t.value());
        assertThat(parsed.getJWTClaimsSet().getClaim("purpose")).isEqualTo("CUSTOM");
    }

    @Test
    @DisplayName("generatePasswordResetToken: adiciona purpose se ausente e typ=PASSWORD_RESET")
    void generatePasswordResetToken_addsPurposeIfAbsent() throws Exception {
        JwtNimbusTokenProviderAdapter a = adapter(propsForTokenFlow("kid-1", "iss", "aud", 0));

        Map<String, Object> claims = new HashMap<>();
        claims.put("channel", "EMAIL");

        JwtToken t = a.generatePasswordResetToken("sub-pr", claims, 60);

        SignedJWT parsed = SignedJWT.parse(t.value());
        JWTClaimsSet cs = parsed.getJWTClaimsSet();

        assertThat(t.type()).isEqualTo(TokenType.PASSWORD_RESET);
        assertThat(cs.getClaim("typ")).isEqualTo("PASSWORD_RESET");
        assertThat(cs.getClaim("purpose")).isEqualTo("PASSWORD_RESET");
        assertThat(cs.getClaim("channel")).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("generateAccessToken: subject null -> NPE")
    void generateAccessToken_subjectNull_throwsNpe() {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-1");

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        assertThatThrownBy(() -> a.generateAccessToken(null, Map.of(), 60))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("subject must not be null");
    }

    @Test
    @DisplayName("generateToken: JOSEException ao assinar -> IllegalStateException(Error signing JWT)")
    void generateToken_signerThrowsJoseException_rethrowsIllegalState() throws Exception {
        JwtNimbusTokenProviderAdapter a = adapter(propsForTokenFlow("kid-1", "iss", "aud", 0));

        JWSSigner badSigner = mock(JWSSigner.class);
        when(badSigner.supportedJWSAlgorithms()).thenReturn(java.util.Set.of(JWSAlgorithm.RS256));
        doThrow(new JOSEException("boom")).when(badSigner).sign(any(JWSHeader.class), any(byte[].class));

        setField(a, "signer", badSigner);

        assertThatThrownBy(() -> a.generateAccessToken("sub", new HashMap<>(), 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error signing JWT")
                .hasCauseInstanceOf(JOSEException.class);
    }

    @Test
    @DisplayName("isValid: token válido (exp futura) -> true")
    void isValid_validToken_returnsTrue() {
        JwtNimbusTokenProviderAdapter a = adapter(propsForTokenFlow("kid-1", "iss", "aud", 0));
        JwtToken t = a.generateAccessToken("sub", new HashMap<>(), 60);

        assertThat(a.isValid(t.value())).isTrue();
    }

    @Test
    @DisplayName("isValid: token expirado (exp no passado) -> false")
    void isValid_expiredToken_returnsFalse() throws Exception {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-1");

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        RSAPrivateKey pk = (RSAPrivateKey) getField(a, "privateKey");
        JWSHeader header = (JWSHeader) getField(a, "jwsHeader");

        JWTClaimsSet cs = new JWTClaimsSet.Builder()
                .subject("sub-exp")
                .issuer("iss")
                .audience("aud")
                .issueTime(Date.from(Instant.now().minusSeconds(120)))
                .expirationTime(Date.from(Instant.now().minusSeconds(60)))
                .claim("typ", "ACCESS")
                .build();

        String raw = signRawJwt(pk, header, cs);

        assertThat(a.isValid(raw)).isFalse();
    }

    @Test
    @DisplayName("isValid: typ inválido não quebra e ainda valida expiração")
    void isValid_invalidTyp_doesNotFail() throws Exception {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-1");

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        RSAPrivateKey pk = (RSAPrivateKey) getField(a, "privateKey");
        JWSHeader header = (JWSHeader) getField(a, "jwsHeader");

        JWTClaimsSet cs = new JWTClaimsSet.Builder()
                .subject("sub")
                .issuer("iss")
                .audience("aud")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .claim("typ", "NOT_A_REAL_TYPE")
                .build();

        String raw = signRawJwt(pk, header, cs);

        assertThat(a.isValid(raw)).isTrue();
    }

    @Test
    @DisplayName("isValid: token inválido (parse error) -> false")
    void isValid_invalidToken_returnsFalse() {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-1");

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        assertThat(a.isValid("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("parseClaims: sucesso retorna mapa com typ + custom claims")
    void parseClaims_success() {
        JwtNimbusTokenProviderAdapter a = adapter(propsForTokenFlow("kid-1", "iss", "aud", 0));

        Map<String, Object> claims = new HashMap<>();
        claims.put("x", "y");

        JwtToken t = a.generateAccessToken("sub", claims, 60);

        Map<String, Object> parsed = a.parseClaims(t.value());

        assertThat(parsed).containsEntry("typ", "ACCESS");
        assertThat(parsed).containsEntry("x", "y");
        assertThat(parsed).containsKey("sub");
        assertThat(parsed).containsKey("iss");
        assertThat(parsed).containsKey("aud");
        assertThat(parsed).containsKey("exp");
        assertThat(parsed).containsKey("iat");
    }

    @Test
    @DisplayName("parseClaims: token inválido -> IllegalArgumentException('Invalid JWT')")
    void parseClaims_invalidToken_throwsIllegalArgumentException() {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-1");

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        assertThatThrownBy(() -> a.parseClaims("invalid.jwt.value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid JWT");
    }

    @Test
    @DisplayName("parseClaims: token sem typ -> resolveTokenType retorna null e não quebra")
    void parseClaims_missingTyp_doesNotFail() throws Exception {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-1");

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        RSAPrivateKey pk = (RSAPrivateKey) getField(a, "privateKey");
        JWSHeader header = (JWSHeader) getField(a, "jwsHeader");

        JWTClaimsSet cs = new JWTClaimsSet.Builder()
                .subject("sub-no-typ")
                .issuer("iss")
                .audience("aud")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .claim("x", 1)
                .build();

        String raw = signRawJwt(pk, header, cs);

        Map<String, Object> parsed = a.parseClaims(raw);

        assertThat(parsed.get("x")).isIn(1, 1L);
        assertThat(parsed).doesNotContainKey("typ");
    }

    @Test
    @DisplayName("toRsaJwk: RSAKey com kid e alg RS256")
    void toRsaJwk_success() throws JOSEException {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-xyz");

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        RSAKey jwk = a.toRsaJwk();

        assertThat(jwk.getKeyID()).isEqualTo("kid-xyz");
        assertThat(jwk.getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(jwk.toRSAPublicKey()).isNotNull();
    }

    @Test
    @DisplayName("loadPrivateKey: KeyPairGenerator.getInstance falha -> IllegalStateException")
    void loadPrivateKey_keyPairGeneratorFails_throwsIllegalState() {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");

        try (MockedStatic<KeyPairGenerator> mocked = mockStatic(KeyPairGenerator.class)) {
            mocked.when(() -> KeyPairGenerator.getInstance("RSA"))
                    .thenThrow(new NoSuchAlgorithmException("no rsa"));

            assertThatThrownBy(() -> new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Could not generate in-memory DEV private key")
                    .hasCauseInstanceOf(NoSuchAlgorithmException.class);
        }
    }

    @Test
    @DisplayName("loadPublicKey: devPublicKey null e KeyPairGenerator.getInstance falha -> IllegalStateException")
    void loadPublicKey_keyPairGeneratorFails_whenDevKeyNull_throwsIllegalState() {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-ok");

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        setField(a, "devPublicKey", null);

        try (MockedStatic<KeyPairGenerator> mocked = mockStatic(KeyPairGenerator.class)) {
            mocked.when(() -> KeyPairGenerator.getInstance("RSA"))
                    .thenThrow(new NoSuchAlgorithmException("no rsa"));

            assertThatThrownBy(() ->
                    invokePrivate(
                            a,
                            "loadPublicKey",
                            new Class<?>[]{ResourceLoader.class, String.class},
                            new Object[]{mock(ResourceLoader.class), "classpath:any"}
                    )
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Could not generate in-memory DEV public key")
                    .hasCauseInstanceOf(NoSuchAlgorithmException.class);
        }
    }

    @Test
    @DisplayName("generateVerificationToken: claims null não adiciona purpose; com claims adiciona via putIfAbsent; não sobrescreve se já existir")
    void generateVerificationToken_putIfAbsentPurpose_branchCoverage() throws Exception {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-1");
        when(p.getIssuer()).thenReturn("iss");
        when(p.getAudience()).thenReturn("aud");
        when(p.getClockSkewSeconds()).thenReturn(0L);

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        JwtToken t1 = a.generateVerificationToken("sub-null", null, 60);
        JWTClaimsSet cs1 = SignedJWT.parse(t1.value()).getJWTClaimsSet();
        assertThat(cs1.getClaim("purpose")).isNull();

        Map<String, Object> claims2 = new HashMap<>();
        JwtToken t2 = a.generateVerificationToken("sub-absent", claims2, 60);

        assertThat(claims2.get("purpose")).isEqualTo("EMAIL_VERIFICATION");
        JWTClaimsSet cs2 = SignedJWT.parse(t2.value()).getJWTClaimsSet();
        assertThat(cs2.getClaim("purpose")).isEqualTo("EMAIL_VERIFICATION");

        Map<String, Object> claims3 = new HashMap<>();
        claims3.put("purpose", "CUSTOM_PURPOSE");
        JwtToken t3 = a.generateVerificationToken("sub-present", claims3, 60);

        assertThat(claims3.get("purpose")).isEqualTo("CUSTOM_PURPOSE");
        JWTClaimsSet cs3 = SignedJWT.parse(t3.value()).getJWTClaimsSet();
        assertThat(cs3.getClaim("purpose")).isEqualTo("CUSTOM_PURPOSE");
    }

    @Test
    @DisplayName("loadPublicKey: cobre branches (location null, devPublicKey != null, devPublicKey == null success, devPublicKey == null exception)")
    void loadPublicKey_fullCoverage_singleTest() throws Exception {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any");
        when(p.getKeyId()).thenReturn("kid-1");

        JwtNimbusTokenProviderAdapter a = new JwtNimbusTokenProviderAdapter(p, mock(ResourceLoader.class));

        Method m = JwtNimbusTokenProviderAdapter.class.getDeclaredMethod("loadPublicKey", ResourceLoader.class, String.class);
        m.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                m.invoke(a, mock(ResourceLoader.class), null);
            } catch (Exception e) {
                throw e.getCause() != null ? e.getCause() : e;
            }
        }).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("public key location must not be null");

        Field f = JwtNimbusTokenProviderAdapter.class.getDeclaredField("devPublicKey");
        f.setAccessible(true);

        RSAPublicKey already = (RSAPublicKey) f.get(a);
        assertThat(already).isNotNull();

        RSAPublicKey r1 = (RSAPublicKey) m.invoke(a, mock(ResourceLoader.class), "classpath:any");
        assertThat(r1).isSameAs(already);

        f.set(a, null);

        RSAPublicKey r2 = (RSAPublicKey) m.invoke(a, mock(ResourceLoader.class), "classpath:any");
        assertThat(r2).isNotNull();
        assertThat((RSAPublicKey) f.get(a)).isSameAs(r2);

        f.set(a, null);

        try (MockedStatic<KeyPairGenerator> mocked = mockStatic(KeyPairGenerator.class)) {
            mocked.when(() -> KeyPairGenerator.getInstance("RSA"))
                    .thenThrow(new NoSuchAlgorithmException("no rsa"));

            assertThatThrownBy(() -> {
                try {
                    m.invoke(a, mock(ResourceLoader.class), "classpath:any");
                } catch (Exception e) {
                    throw e.getCause() != null ? e.getCause() : e;
                }
            }).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Could not generate in-memory DEV public key")
                    .hasCauseInstanceOf(NoSuchAlgorithmException.class);
        }
    }

    // heapers

    private JwtNimbusTokenProviderAdapter adapter(JwtProperties props) {
        return new JwtNimbusTokenProviderAdapter(props, mock(ResourceLoader.class));
    }

    private JwtProperties propsMinimalForConstructor(String privateLoc, String publicLoc, String keyId) {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn(privateLoc);
        when(p.getPublicKeyLocation()).thenReturn(publicLoc);
        when(p.getKeyId()).thenReturn(keyId);
        return p;
    }

    private JwtProperties propsForTokenFlow(String keyId, String issuer, String audience, long skewSeconds) {
        JwtProperties p = mock(JwtProperties.class);
        when(p.getPrivateKeyLocation()).thenReturn("classpath:any-private");
        when(p.getPublicKeyLocation()).thenReturn("classpath:any-public");
        when(p.getKeyId()).thenReturn(keyId);
        when(p.getIssuer()).thenReturn(issuer);
        when(p.getAudience()).thenReturn(audience);
        when(p.getClockSkewSeconds()).thenReturn(skewSeconds);
        return p;
    }

    private static Object getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            try {
                Field modifiers = Field.class.getDeclaredField("modifiers");
                modifiers.setAccessible(true);
                modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            } catch (Exception ignored) {
            }
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invokePrivate(Object target, String methodName, Class<?>[] types, Object[] args) {
        try {
            Method m = target.getClass().getDeclaredMethod(methodName, types);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Exception e) {
            Throwable t = e.getCause() != null ? e.getCause() : e;
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Error err) throw err;
            throw new RuntimeException(t);
        }
    }

    private static String signRawJwt(RSAPrivateKey privateKey, JWSHeader header, JWTClaimsSet claims) throws JOSEException {
        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new RSASSASigner(privateKey));
        return jwt.serialize();
    }
}