package br.com.ecofy.auth.adapters.out.jwt;

import br.com.ecofy.auth.config.JwtProperties;
import br.com.ecofy.auth.core.domain.JwtToken;
import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.domain.keys.ActiveSigningKey;
import br.com.ecofy.auth.core.domain.keys.SigningKeyMetadata;
import br.com.ecofy.auth.core.domain.keys.VerificationKey;
import br.com.ecofy.auth.core.port.out.SigningKeyProviderPort;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.lang.reflect.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Testes unitários do adaptador Nimbus de tokens JWT")
class JwtNimbusTokenProviderAdapterTest {

    private static final String ACTIVE_KID = "active-key";
    private static final String RETIRING_KID = "retiring-key";
    private static final String ISSUER = "https://auth.ecofy.com";
    private static final String AUDIENCE = "ecofy-api";
    private static final String SUBJECT = "21f71895-45a1-4509-a2a9-a96a79c27257";

    private JwtProperties jwtProperties;
    private SigningKeyProviderPort signingKeyProvider;
    private KeyPair activeKeyPair;
    private ActiveSigningKey activeSigningKey;
    private VerificationKey activeVerificationKey;

    @BeforeEach
    void setUp() throws Exception {
        jwtProperties = mock(JwtProperties.class);
        signingKeyProvider = mock(SigningKeyProviderPort.class);
        activeKeyPair = generateRsaKeyPair();

        SigningKeyMetadata metadata = new SigningKeyMetadata(
                ACTIVE_KID,
                "RS256",
                SigningKeyMetadata.Status.ACTIVE,
                Instant.now(),
                null,
                null
        );

        activeSigningKey = new ActiveSigningKey(
                metadata,
                (RSAPrivateKey) activeKeyPair.getPrivate(),
                (RSAPublicKey) activeKeyPair.getPublic()
        );

        activeVerificationKey = new VerificationKey(
                metadata,
                (RSAPublicKey) activeKeyPair.getPublic()
        );

        when(jwtProperties.getIssuer()).thenReturn(ISSUER);
        when(jwtProperties.getAudience()).thenReturn(AUDIENCE);
        when(signingKeyProvider.activeKey()).thenReturn(activeSigningKey);
        when(signingKeyProvider.verificationKeys())
                .thenReturn(List.of(activeVerificationKey));
    }

    @Test
    @DisplayName("Deve rejeitar propriedades JWT nulas ao construir o adaptador")
    void constructor_jwtPropertiesNulo_deveLancarNullPointerException() {
        // Arrange
        JwtProperties nullProperties = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwtNimbusTokenProviderAdapter(
                        nullProperties,
                        signingKeyProvider
                )
        );

        // Assert
        assertEquals(
                "jwtProperties must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o provedor de chaves nulo ao construir o adaptador")
    void constructor_signingKeyProviderNulo_deveLancarNullPointerException() {
        // Arrange
        SigningKeyProviderPort nullProvider = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new JwtNimbusTokenProviderAdapter(
                        jwtProperties,
                        nullProvider
                )
        );

        // Assert
        assertEquals(
                "signingKeyProvider must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve construir o adaptador com emissor configurado")
    void constructor_issuerValido_deveCriarDecoderComValidadorDeEmissor() {
        // Arrange
        when(jwtProperties.getIssuer()).thenReturn(ISSUER);

        // Act
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Assert
        assertNotNull(adapter.jwtDecoder());
    }

    @Test
    @DisplayName("Deve construir o adaptador sem validador de emissor quando o valor for nulo")
    void constructor_issuerNulo_deveCriarDecoderSemValidadorDeEmissor() {
        // Arrange
        when(jwtProperties.getIssuer()).thenReturn(null);

        // Act
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Assert
        assertNotNull(adapter.jwtDecoder());
    }

    @Test
    @DisplayName("Deve construir o adaptador sem validador de emissor quando o valor estiver em branco")
    void constructor_issuerEmBranco_deveCriarDecoderSemValidadorDeEmissor() {
        // Arrange
        when(jwtProperties.getIssuer()).thenReturn("   ");

        // Act
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Assert
        assertNotNull(adapter.jwtDecoder());
    }

    @Test
    @DisplayName("Deve gerar token de acesso assinado com as claims obrigatórias e personalizadas")
    void generateAccessToken_dadosValidos_deveGerarTokenDeAcessoAssinado()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("email", "usuario@ecofy.com");
        customClaims.put("role", "USER");

        // Act
        JwtToken result = adapter.generateAccessToken(
                SUBJECT,
                customClaims,
                300
        );

        SignedJWT signedJWT = parse(result);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(
                        JWSAlgorithm.RS256,
                        signedJWT.getHeader().getAlgorithm()
                ),
                () -> assertEquals(
                        JOSEObjectType.JWT,
                        signedJWT.getHeader().getType()
                ),
                () -> assertEquals(
                        ACTIVE_KID,
                        signedJWT.getHeader().getKeyID()
                ),
                () -> assertEquals(SUBJECT, claims.getSubject()),
                () -> assertEquals(ISSUER, claims.getIssuer()),
                () -> assertEquals(
                        List.of(AUDIENCE),
                        claims.getAudience()
                ),
                () -> assertEquals(
                        TokenType.ACCESS.name(),
                        claims.getStringClaim("typ")
                ),
                () -> assertEquals(
                        SUBJECT,
                        claims.getStringClaim("authUserId")
                ),
                () -> assertEquals(
                        "usuario@ecofy.com",
                        claims.getStringClaim("email")
                ),
                () -> assertEquals(
                        "USER",
                        claims.getStringClaim("role")
                ),
                () -> assertNotNull(claims.getJWTID()),
                () -> assertNotNull(claims.getIssueTime()),
                () -> assertNotNull(claims.getNotBeforeTime()),
                () -> assertNotNull(claims.getExpirationTime()),
                () -> assertEquals(
                        claims.getIssueTime().toInstant().plusSeconds(300),
                        claims.getExpirationTime().toInstant()
                )
        );
    }

    @Test
    @DisplayName("Deve gerar token de acesso sem claims personalizadas quando o mapa for nulo")
    void generateAccessToken_claimsNulas_deveGerarTokenSemClaimsPersonalizadas()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        JwtToken result = adapter.generateAccessToken(
                SUBJECT,
                null,
                300
        );

        JWTClaimsSet claims = parse(result).getJWTClaimsSet();

        // Assert
        assertAll(
                () -> assertEquals(
                        TokenType.ACCESS.name(),
                        claims.getStringClaim("typ")
                ),
                () -> assertEquals(
                        SUBJECT,
                        claims.getStringClaim("authUserId")
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar assunto nulo ao gerar token de acesso")
    void generateAccessToken_subjectNulo_deveLancarNullPointerException() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.generateAccessToken(
                        null,
                        Map.of(),
                        300
                )
        );

        // Assert
        assertEquals(
                "subject must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve gerar token de atualização sem adicionar a claim authUserId")
    void generateRefreshToken_dadosValidos_deveGerarTokenDeAtualizacao()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        Map<String, Object> customClaims = Map.of(
                "sessionId",
                "session-123"
        );

        // Act
        JwtToken result = adapter.generateRefreshToken(
                SUBJECT,
                customClaims,
                600
        );

        JWTClaimsSet claims = parse(result).getJWTClaimsSet();

        // Assert
        assertAll(
                () -> assertEquals(
                        TokenType.REFRESH.name(),
                        claims.getStringClaim("typ")
                ),
                () -> assertEquals(
                        "session-123",
                        claims.getStringClaim("sessionId")
                ),
                () -> assertNull(claims.getClaim("authUserId")),
                () -> assertEquals(
                        claims.getIssueTime().toInstant().plusSeconds(600),
                        claims.getExpirationTime().toInstant()
                )
        );
    }

    @Test
    @DisplayName("Deve gerar token expirado quando o tempo de vida informado for negativo")
    void generateRefreshToken_ttlNegativo_deveGerarTokenJaExpirado()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        JwtToken result = adapter.generateRefreshToken(
                SUBJECT,
                Map.of(),
                -1
        );

        SignedJWT signedJWT = parse(result);

        // Assert
        assertAll(
                () -> assertTrue(
                        signedJWT.getJWTClaimsSet()
                                .getExpirationTime()
                                .before(signedJWT.getJWTClaimsSet().getIssueTime())
                ),
                () -> assertFalse(adapter.isValid(serializedToken(result)))
        );
    }

    @Test
    @DisplayName("Deve adicionar o propósito de confirmação de e-mail quando ele não estiver presente")
    void generateVerificationToken_semPurpose_deveAdicionarPurposeDeConfirmacao()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        Map<String, Object> claims = new HashMap<>();

        // Act
        JwtToken result = adapter.generateVerificationToken(
                SUBJECT,
                claims,
                300
        );

        JWTClaimsSet tokenClaims = parse(result).getJWTClaimsSet();

        // Assert
        assertAll(
                () -> assertEquals(
                        "EMAIL_VERIFICATION",
                        claims.get("purpose")
                ),
                () -> assertEquals(
                        "EMAIL_VERIFICATION",
                        tokenClaims.getStringClaim("purpose")
                ),
                () -> assertEquals(
                        TokenType.VERIFICATION.name(),
                        tokenClaims.getStringClaim("typ")
                ),
                () -> assertNull(tokenClaims.getClaim("authUserId"))
        );
    }

    @Test
    @DisplayName("Deve preservar o propósito existente ao gerar token de confirmação")
    void generateVerificationToken_purposeExistente_devePreservarValorInformado()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "CUSTOM_PURPOSE");

        // Act
        JwtToken result = adapter.generateVerificationToken(
                SUBJECT,
                claims,
                300
        );

        // Assert
        assertEquals(
                "CUSTOM_PURPOSE",
                parse(result).getJWTClaimsSet().getStringClaim("purpose")
        );
    }

    @Test
    @DisplayName("Deve gerar token de confirmação sem propósito quando as claims forem nulas")
    void generateVerificationToken_claimsNulas_deveGerarTokenSemPurpose()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        JwtToken result = adapter.generateVerificationToken(
                SUBJECT,
                null,
                300
        );

        JWTClaimsSet claims = parse(result).getJWTClaimsSet();

        // Assert
        assertAll(
                () -> assertEquals(
                        TokenType.VERIFICATION.name(),
                        claims.getStringClaim("typ")
                ),
                () -> assertNull(claims.getClaim("purpose"))
        );
    }

    @Test
    @DisplayName("Deve adicionar o propósito de redefinição de senha quando ele não estiver presente")
    void generatePasswordResetToken_semPurpose_deveAdicionarPurposeDeRedefinicao()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        Map<String, Object> claims = new HashMap<>();

        // Act
        JwtToken result = adapter.generatePasswordResetToken(
                SUBJECT,
                claims,
                300
        );

        JWTClaimsSet tokenClaims = parse(result).getJWTClaimsSet();

        // Assert
        assertAll(
                () -> assertEquals(
                        "PASSWORD_RESET",
                        claims.get("purpose")
                ),
                () -> assertEquals(
                        "PASSWORD_RESET",
                        tokenClaims.getStringClaim("purpose")
                ),
                () -> assertEquals(
                        TokenType.PASSWORD_RESET.name(),
                        tokenClaims.getStringClaim("typ")
                ),
                () -> assertNull(tokenClaims.getClaim("authUserId"))
        );
    }

    @Test
    @DisplayName("Deve preservar o propósito existente ao gerar token de redefinição de senha")
    void generatePasswordResetToken_purposeExistente_devePreservarValorInformado()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "CUSTOM_RESET");

        // Act
        JwtToken result = adapter.generatePasswordResetToken(
                SUBJECT,
                claims,
                300
        );

        // Assert
        assertEquals(
                "CUSTOM_RESET",
                parse(result).getJWTClaimsSet().getStringClaim("purpose")
        );
    }

    @Test
    @DisplayName("Deve gerar token de redefinição sem propósito quando as claims forem nulas")
    void generatePasswordResetToken_claimsNulas_deveGerarTokenSemPurpose()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        JwtToken result = adapter.generatePasswordResetToken(
                SUBJECT,
                null,
                300
        );

        JWTClaimsSet claims = parse(result).getJWTClaimsSet();

        // Assert
        assertAll(
                () -> assertEquals(
                        TokenType.PASSWORD_RESET.name(),
                        claims.getStringClaim("typ")
                ),
                () -> assertNull(claims.getClaim("purpose"))
        );
    }

    @Test
    @DisplayName("Deve rejeitar tipo de token nulo durante a geração")
    void generateToken_tipoNulo_deveLancarNullPointerException() throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        Method method = JwtNimbusTokenProviderAdapter.class.getDeclaredMethod(
                "generateToken",
                String.class,
                Map.class,
                long.class,
                TokenType.class
        );
        method.setAccessible(true);

        // Act
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(
                        adapter,
                        SUBJECT,
                        Map.of(),
                        300L,
                        null
                )
        );

        // Assert
        assertAll(
                () -> assertInstanceOf(
                        NullPointerException.class,
                        exception.getCause()
                ),
                () -> assertEquals(
                        "type must not be null",
                        exception.getCause().getMessage()
                )
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando ocorrer erro ao assinar o token")
    void generateAccessToken_falhaNaAssinatura_deveLancarIllegalStateException()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        JOSEException cause = new JOSEException("Falha ao assinar");
        JWSSigner failingSigner = mock(JWSSigner.class);

        when(failingSigner.supportedJWSAlgorithms())
                .thenReturn(Set.of(JWSAlgorithm.RS256));
        when(failingSigner.sign(any(JWSHeader.class), any(byte[].class)))
                .thenThrow(cause);

        setPrivateField(adapter, "signer", failingSigner);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.generateAccessToken(
                        SUBJECT,
                        Map.of(),
                        300
                )
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "Error signing JWT",
                        exception.getMessage()
                ),
                () -> assertSame(cause, exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve considerar válido um token com data de expiração futura")
    void isValid_tokenNaoExpirado_deveRetornarTrue() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        JwtToken token = adapter.generateAccessToken(
                SUBJECT,
                Map.of(),
                300
        );

        // Act
        boolean result = adapter.isValid(serializedToken(token));

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar inválido um token com data de expiração passada")
    void isValid_tokenExpirado_deveRetornarFalse() throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        String token = createSignedToken(
                activeKeyPair,
                ACTIVE_KID,
                ISSUER,
                Date.from(Instant.now().minusSeconds(300))
        );

        // Act
        boolean result = adapter.isValid(token);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar inválido um token sem data de expiração")
    void isValid_tokenSemExpiracao_deveRetornarFalse() throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        String token = createSignedToken(
                activeKeyPair,
                ACTIVE_KID,
                ISSUER,
                null
        );

        // Act
        boolean result = adapter.isValid(token);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar inválido um conteúdo que não represente um JWT")
    void isValid_tokenMalformado_deveRetornarFalse() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        boolean result = adapter.isValid("token-invalido");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar inválido um token nulo")
    void isValid_tokenNulo_deveRetornarFalse() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        boolean result = adapter.isValid(null);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve extrair todas as claims de um token sintaticamente válido")
    void parseClaims_tokenValido_deveRetornarClaims() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        JwtToken token = adapter.generateAccessToken(
                SUBJECT,
                Map.of("email", "usuario@ecofy.com"),
                300
        );

        // Act
        Map<String, Object> result = adapter.parseClaims(
                serializedToken(token)
        );

        // Assert
        assertAll(
                () -> assertEquals(SUBJECT, result.get("sub")),
                () -> assertEquals("usuario@ecofy.com", result.get("email")),
                () -> assertEquals(TokenType.ACCESS.name(), result.get("typ")),
                () -> assertEquals(SUBJECT, result.get("authUserId"))
        );
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar extrair claims de um token malformado")
    void parseClaims_tokenMalformado_deveLancarIllegalArgumentException() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.parseClaims("token-invalido")
        );

        // Assert
        assertAll(
                () -> assertEquals("Invalid JWT", exception.getMessage()),
                () -> assertNotNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve verificar a assinatura e retornar uma cópia mutável das claims")
    void verifyAndParseClaims_tokenValido_deveRetornarCopiaMutavelDasClaims() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        JwtToken token = adapter.generateAccessToken(
                SUBJECT,
                Map.of("role", "USER"),
                300
        );

        // Act
        Map<String, Object> result = adapter.verifyAndParseClaims(
                serializedToken(token)
        );
        result.put("novaClaim", "novoValor");

        // Assert
        assertAll(
                () -> assertInstanceOf(HashMap.class, result),
                () -> assertEquals(SUBJECT, result.get("sub")),
                () -> assertEquals("USER", result.get("role")),
                () -> assertEquals("novoValor", result.get("novaClaim"))
        );
    }

    @Test
    @DisplayName("Deve rejeitar token assinado por uma chave não reconhecida")
    void verifyAndParseClaims_assinaturaDesconhecida_deveLancarIllegalArgumentException()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        KeyPair unknownKeyPair = generateRsaKeyPair();
        String token = createSignedToken(
                unknownKeyPair,
                "unknown-key",
                ISSUER,
                Date.from(Instant.now().plusSeconds(300))
        );

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.verifyAndParseClaims(token)
        );

        // Assert
        assertAll(
                () -> assertEquals("Invalid token", exception.getMessage()),
                () -> assertNotNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve rejeitar token emitido por um emissor diferente do configurado")
    void verifyAndParseClaims_issuerDiferente_deveLancarIllegalArgumentException() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        when(jwtProperties.getIssuer()).thenReturn(
                "https://outro-emissor.ecofy.com"
        );

        JwtToken token = adapter.generateAccessToken(
                SUBJECT,
                Map.of(),
                300
        );

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.verifyAndParseClaims(
                        serializedToken(token)
                )
        );

        // Assert
        assertAll(
                () -> assertEquals("Invalid token", exception.getMessage()),
                () -> assertNotNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve rejeitar token expirado durante a validação criptográfica")
    void verifyAndParseClaims_tokenExpirado_deveLancarIllegalArgumentException()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();
        String token = createSignedToken(
                activeKeyPair,
                ACTIVE_KID,
                ISSUER,
                Date.from(Instant.now().minusSeconds(300))
        );

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.verifyAndParseClaims(token)
        );

        // Assert
        assertAll(
                () -> assertEquals("Invalid token", exception.getMessage()),
                () -> assertNotNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve rejeitar conteúdo malformado durante a validação do token")
    void verifyAndParseClaims_tokenMalformado_deveLancarIllegalArgumentException() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.verifyAndParseClaims("token-invalido")
        );

        // Assert
        assertAll(
                () -> assertEquals("Invalid token", exception.getMessage()),
                () -> assertNotNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve rejeitar token nulo antes de executar a validação")
    void verifyAndParseClaims_tokenNulo_deveLancarNullPointerException() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.verifyAndParseClaims(null)
        );

        // Assert
        assertEquals(
                "token must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve validar token sem emissor quando nenhum emissor estiver configurado")
    void verifyAndParseClaims_semIssuerConfigurado_deveRetornarClaims() {
        // Arrange
        when(jwtProperties.getIssuer()).thenReturn(null);
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        JwtToken token = adapter.generateAccessToken(
                SUBJECT,
                Map.of(),
                300
        );

        // Act
        Map<String, Object> result = adapter.verifyAndParseClaims(
                serializedToken(token)
        );

        // Assert
        assertEquals(SUBJECT, result.get("sub"));
    }

    @Test
    @DisplayName("Deve retornar as chaves públicas de verificação no formato JWKS")
    void currentPublicJwks_chavesDisponiveis_deveRetornarJwksPublicos()
            throws Exception {
        // Arrange
        KeyPair retiringKeyPair = generateRsaKeyPair();
        VerificationKey retiringKey = createRetiringVerificationKey(
                retiringKeyPair
        );

        when(signingKeyProvider.verificationKeys()).thenReturn(
                List.of(activeVerificationKey, retiringKey)
        );

        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        List<Map<String, Object>> result = adapter.currentPublicJwks();

        // Assert
        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(ACTIVE_KID, result.get(0).get("kid")),
                () -> assertEquals(RETIRING_KID, result.get(1).get("kid")),
                () -> assertEquals("RSA", result.get(0).get("kty")),
                () -> assertEquals("RS256", result.get(0).get("alg")),
                () -> assertEquals("sig", result.get(0).get("use")),
                () -> assertNotNull(result.get(0).get("n")),
                () -> assertNotNull(result.get(0).get("e")),
                () -> assertFalse(result.get(0).containsKey("d")),
                () -> assertFalse(result.get(1).containsKey("d"))
        );
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver chaves públicas de verificação")
    void currentPublicJwks_semChavesDeVerificacao_deveRetornarListaVazia() {
        // Arrange
        when(signingKeyProvider.verificationKeys()).thenReturn(List.of());
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        List<Map<String, Object>> result = adapter.currentPublicJwks();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar sempre a mesma instância do decoder configurado")
    void jwtDecoder_adaptadorInicializado_deveRetornarMesmaInstancia() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        NimbusJwtDecoder firstResult = adapter.jwtDecoder();
        NimbusJwtDecoder secondResult = adapter.jwtDecoder();

        // Assert
        assertAll(
                () -> assertNotNull(firstResult),
                () -> assertSame(firstResult, secondResult)
        );
    }

    @Test
    @DisplayName("Deve converter a chave ativa para uma JWK pública RSA")
    void toRsaJwk_chaveAtivaDisponivel_deveRetornarJwkPublica() {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        RSAKey result = adapter.toRsaJwk();

        // Assert
        assertAll(
                () -> assertEquals(ACTIVE_KID, result.getKeyID()),
                () -> assertEquals(JWSAlgorithm.RS256, result.getAlgorithm()),
                () -> assertEquals(KeyUse.SIGNATURE, result.getKeyUse()),
                () -> assertEquals(
                        ((RSAPublicKey) activeKeyPair.getPublic()).getModulus(),
                        result.toRSAPublicKey().getModulus()
                ),
                () -> assertFalse(result.isPrivate())
        );
    }

    @Test
    @DisplayName("Deve gerar identificadores JWT diferentes para tokens distintos")
    void generateAccessToken_geracoesConsecutivas_deveGerarJwtIdsDiferentes()
            throws Exception {
        // Arrange
        JwtNimbusTokenProviderAdapter adapter = createAdapter();

        // Act
        JwtToken firstToken = adapter.generateAccessToken(
                SUBJECT,
                Map.of(),
                300
        );
        JwtToken secondToken = adapter.generateAccessToken(
                SUBJECT,
                Map.of(),
                300
        );

        String firstJwtId = parse(firstToken)
                .getJWTClaimsSet()
                .getJWTID();
        String secondJwtId = parse(secondToken)
                .getJWTClaimsSet()
                .getJWTID();

        // Assert
        assertNotEquals(firstJwtId, secondJwtId);
    }

    private JwtNimbusTokenProviderAdapter createAdapter() {
        return new JwtNimbusTokenProviderAdapter(
                jwtProperties,
                signingKeyProvider
        );
    }

    private VerificationKey createRetiringVerificationKey(KeyPair keyPair) {
        SigningKeyMetadata metadata = new SigningKeyMetadata(
                RETIRING_KID,
                "RS256",
                SigningKeyMetadata.Status.RETIRING,
                null,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new VerificationKey(
                metadata,
                (RSAPublicKey) keyPair.getPublic()
        );
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static SignedJWT parse(JwtToken token) throws Exception {
        return SignedJWT.parse(serializedToken(token));
    }

    private static String serializedToken(JwtToken token) {
        try {
            for (Field field : JwtToken.class.getDeclaredFields()) {
                if (field.getType() == String.class
                        && !Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    return (String) field.get(token);
                }
            }

            throw new IllegalStateException(
                    "JwtToken não possui campo de token do tipo String"
            );
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Não foi possível obter o valor serializado do token",
                    e
            );
        }
    }

    private static String createSignedToken(
            KeyPair keyPair,
            String kid,
            String issuer,
            Date expiration
    ) throws Exception {
        Instant now = Instant.now();

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .subject(SUBJECT)
                .issuer(issuer)
                .audience(AUDIENCE)
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(1)))
                .jwtID("jwt-" + now.toEpochMilli());

        if (expiration != null) {
            claimsBuilder.expirationTime(expiration);
        }

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(kid)
                        .type(JOSEObjectType.JWT)
                        .build(),
                claimsBuilder.build()
        );

        signedJWT.sign(
                new RSASSASigner(
                        (RSAPrivateKey) keyPair.getPrivate()
                )
        );

        return signedJWT.serialize();
    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value
    ) throws Exception {
        Field field = JwtNimbusTokenProviderAdapter.class.getDeclaredField(
                fieldName
        );
        field.setAccessible(true);
        field.set(target, value);
    }
}
