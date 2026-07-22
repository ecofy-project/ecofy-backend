package br.com.ecofy.auth.adapters.out.jwt;

import br.com.ecofy.auth.config.KeysProperties;
import br.com.ecofy.auth.core.domain.keys.ActiveSigningKey;
import br.com.ecofy.auth.core.domain.keys.VerificationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Testes unitários do provedor configurável de chaves de assinatura")
class ConfigurableSigningKeyProviderTest {

    private static final String ACTIVE_KID = "active-key";
    private static final String RETIRING_KID = "retiring-key";
    private static final String ALGORITHM = "RS256";
    private static final Instant NOW = Instant.parse("2026-07-20T12:00:00Z");
    private static final Duration RETENTION_WINDOW = Duration.ofDays(30);

    private KeysProperties properties;
    private ResourceLoader resourceLoader;
    private Clock clock;

    @BeforeEach
    void setUp() {
        properties = mock(KeysProperties.class);
        resourceLoader = mock(ResourceLoader.class);
        clock = mock(Clock.class);

        when(properties.getAlgorithm()).thenReturn(ALGORITHM);
        when(properties.getActiveKid()).thenReturn(ACTIVE_KID);
        when(properties.getActivePrivateKey()).thenReturn(null);
        when(properties.getActivePrivateKeyLocation()).thenReturn(null);
        when(properties.isAllowGeneratedKey()).thenReturn(true);
        when(properties.getRetentionWindow()).thenReturn(RETENTION_WINDOW);
        when(properties.getRetiring()).thenReturn(List.of());
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    @DisplayName("Deve gerar um par RSA temporário quando não houver chave configurada e a geração estiver permitida")
    void constructor_semChaveConfiguradaEGeracaoPermitida_deveGerarParRsaTemporario() {
        // Arrange
        when(properties.getActivePrivateKeyLocation()).thenReturn(" ");

        // Act
        ConfigurableSigningKeyProvider provider = createProvider();
        ActiveSigningKey activeKey = provider.activeKey();
        List<VerificationKey> verificationKeys = provider.verificationKeys();

        // Assert
        assertAll(
                () -> assertEquals(ACTIVE_KID, activeKey.kid()),
                () -> assertNotNull(activeKey.privateKey()),
                () -> assertNotNull(activeKey.publicKey()),
                () -> assertEquals(2048, activeKey.publicKey().getModulus().bitLength()),
                () -> assertEquals(1, verificationKeys.size()),
                () -> assertEquals(ACTIVE_KID, verificationKeys.getFirst().metadata().kid()),
                () -> verify(resourceLoader, never()).getResource(" ")
        );
    }

    @Test
    @DisplayName("Deve retornar sempre a mesma chave ativa resolvida durante a inicialização")
    void activeKey_provedorInicializado_deveRetornarMesmaChaveAtiva() {
        // Arrange
        ConfigurableSigningKeyProvider provider = createProvider();

        // Act
        ActiveSigningKey firstResult = provider.activeKey();
        ActiveSigningKey secondResult = provider.activeKey();

        // Assert
        assertSame(firstResult, secondResult);
    }

    @Test
    @DisplayName("Deve carregar a chave privada informada diretamente e derivar sua chave pública")
    void constructor_chavePrivadaInlineValida_deveCarregarChaveEDerivarPublica() throws Exception {
        // Arrange
        KeyPair pair = generateRsaKeyPair();
        String privatePem = toPrivatePem(pair);

        when(properties.getActivePrivateKey()).thenReturn(privatePem);
        when(properties.isAllowGeneratedKey()).thenReturn(false);

        // Act
        ConfigurableSigningKeyProvider provider = createProvider();
        ActiveSigningKey activeKey = provider.activeKey();

        // Assert
        RSAPublicKey expectedPublicKey = (RSAPublicKey) pair.getPublic();

        assertAll(
                () -> assertEquals(ACTIVE_KID, activeKey.kid()),
                () -> assertEquals(
                        expectedPublicKey.getModulus(),
                        activeKey.publicKey().getModulus()
                ),
                () -> assertEquals(
                        expectedPublicKey.getPublicExponent(),
                        activeKey.publicKey().getPublicExponent()
                ),
                () -> verify(resourceLoader, never()).getResource(
                        properties.getActivePrivateKeyLocation()
                )
        );
    }

    @Test
    @DisplayName("Deve carregar a chave privada pela localização configurada quando não houver conteúdo inline")
    void constructor_chavePrivadaEmRecursoExistente_deveCarregarConteudoDoRecurso() throws Exception {
        // Arrange
        String location = "file:/run/secrets/active-private-key.pem";
        KeyPair pair = generateRsaKeyPair();
        String privatePem = toPrivatePem(pair);
        Resource resource = mock(Resource.class);

        when(properties.getActivePrivateKey()).thenReturn(" ");
        when(properties.getActivePrivateKeyLocation()).thenReturn(location);
        when(properties.isAllowGeneratedKey()).thenReturn(false);
        when(resourceLoader.getResource(location)).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(
                new ByteArrayInputStream(privatePem.getBytes(StandardCharsets.UTF_8))
        );

        // Act
        ConfigurableSigningKeyProvider provider = createProvider();

        // Assert
        assertAll(
                () -> assertEquals(ACTIVE_KID, provider.activeKey().kid()),
                () -> assertEquals(
                        ((RSAPublicKey) pair.getPublic()).getModulus(),
                        provider.activeKey().publicKey().getModulus()
                ),
                () -> verify(resourceLoader).getResource(location),
                () -> verify(resource).getInputStream()
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando a localização da chave ativa não existir")
    void constructor_recursoDaChaveAtivaInexistente_deveLancarExcecao() {
        // Arrange
        String location = "file:/run/secrets/inexistente.pem";
        Resource resource = mock(Resource.class);

        when(properties.getActivePrivateKeyLocation()).thenReturn(location);
        when(resourceLoader.getResource(location)).thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains(
                                "chave privada ativa não encontrada na localização configurada"
                        )
                ),
                () -> verify(resourceLoader).getResource(location),
                () -> verify(resource, never()).getInputStream()
        );
    }

    @Test
    @DisplayName("Deve lançar exceção com a causa original quando ocorrer erro ao ler a chave ativa")
    void constructor_falhaAoLerRecursoDaChaveAtiva_deveLancarExcecaoComCausa() throws Exception {
        // Arrange
        String location = "file:/run/secrets/active-private-key.pem";
        IOException cause = new IOException("Falha de leitura");
        Resource resource = mock(Resource.class);

        when(properties.getActivePrivateKeyLocation()).thenReturn(location);
        when(resourceLoader.getResource(location)).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenThrow(cause);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "Failed to read signing key material: chave privada ativa",
                        exception.getMessage()
                ),
                () -> assertSame(cause, exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve rejeitar algoritmo nulo por não ser permitido para assinatura")
    void constructor_algoritmoNulo_deveLancarExcecao() {
        // Arrange
        when(properties.getAlgorithm()).thenReturn(null);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertEquals(
                "ECO-20: algoritmo de assinatura não permitido: null (permitido: RS256).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar algoritmo diferente de RS256")
    void constructor_algoritmoNaoPermitido_deveLancarExcecao() {
        // Arrange
        when(properties.getAlgorithm()).thenReturn("HS256");

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertEquals(
                "ECO-20: algoritmo de assinatura não permitido: HS256 (permitido: RS256).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar identificador nulo para a chave ativa")
    void constructor_kidAtivoNulo_deveLancarExcecao() {
        // Arrange
        when(properties.getActiveKid()).thenReturn(null);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertEquals(
                "ECO-20: 'ecofy.auth.keys.active-kid' é obrigatório "
                        + "(o kid vai no header do JWT e no JWKS).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar identificador em branco para a chave ativa")
    void constructor_kidAtivoEmBranco_deveLancarExcecao() {
        // Arrange
        when(properties.getActiveKid()).thenReturn("   ");

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertEquals(
                "ECO-20: 'ecofy.auth.keys.active-kid' é obrigatório "
                        + "(o kid vai no header do JWT e no JWKS).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar ausência de chave quando a geração temporária estiver desabilitada")
    void constructor_semChaveEGeracaoDesabilitada_deveLancarExcecao() {
        // Arrange
        when(properties.isAllowGeneratedKey()).thenReturn(false);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertEquals(
                "ECO-20: nenhuma chave de assinatura configurada e a geração em memória "
                        + "está desabilitada (ecofy.auth.keys.allow-generated-key=false). "
                        + "Configure 'active-private-key' ou 'active-private-key-location' "
                        + "via secret externo.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve incluir uma chave aposentada válida entre as chaves de verificação")
    void verificationKeys_chaveAposentadaValida_deveRetornarChaveAtivaEAposentada()
            throws Exception {
        // Arrange
        KeyPair retiringPair = generateRsaKeyPair();
        KeysProperties.RetiringKey retiringKey = createRetiringKey(
                RETIRING_KID,
                toPublicPem(retiringPair),
                null
        );

        when(properties.getRetiring()).thenReturn(List.of(retiringKey));

        ConfigurableSigningKeyProvider provider = createProvider();

        // Act
        List<VerificationKey> result = provider.verificationKeys();

        // Assert
        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(ACTIVE_KID, result.get(0).metadata().kid()),
                () -> assertEquals(RETIRING_KID, result.get(1).metadata().kid()),
                () -> assertEquals(
                        ((RSAPublicKey) retiringPair.getPublic()).getModulus(),
                        result.get(1).publicKey().getModulus()
                )
        );
    }

    @Test
    @DisplayName("Deve carregar a chave pública aposentada pela localização configurada")
    void constructor_chaveAposentadaEmRecursoExistente_deveCarregarConteudoDoRecurso()
            throws Exception {
        // Arrange
        String location = "file:/run/secrets/retiring-public-key.pem";
        KeyPair retiringPair = generateRsaKeyPair();
        String publicPem = toPublicPem(retiringPair);
        Resource resource = mock(Resource.class);
        KeysProperties.RetiringKey retiringKey = createRetiringKey(
                RETIRING_KID,
                " ",
                location
        );

        when(properties.getRetiring()).thenReturn(List.of(retiringKey));
        when(resourceLoader.getResource(location)).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(
                new ByteArrayInputStream(publicPem.getBytes(StandardCharsets.UTF_8))
        );

        // Act
        ConfigurableSigningKeyProvider provider = createProvider();
        List<VerificationKey> result = provider.verificationKeys();

        // Assert
        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(RETIRING_KID, result.get(1).metadata().kid()),
                () -> assertEquals(
                        ((RSAPublicKey) retiringPair.getPublic()).getModulus(),
                        result.get(1).publicKey().getModulus()
                ),
                () -> verify(resourceLoader).getResource(location)
        );
    }

    @Test
    @DisplayName("Deve remover a chave aposentada quando sua janela de retenção expirar")
    void verificationKeys_janelaDeRetencaoExpirada_deveRetornarSomenteChaveAtiva()
            throws Exception {
        // Arrange
        KeyPair retiringPair = generateRsaKeyPair();
        KeysProperties.RetiringKey retiringKey = createRetiringKey(
                RETIRING_KID,
                toPublicPem(retiringPair),
                null
        );

        when(properties.getRetiring()).thenReturn(List.of(retiringKey));
        when(clock.instant()).thenReturn(
                NOW,
                NOW.plus(RETENTION_WINDOW).plusSeconds(1)
        );

        ConfigurableSigningKeyProvider provider = createProvider();

        // Act
        List<VerificationKey> result = provider.verificationKeys();

        // Assert
        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(ACTIVE_KID, result.getFirst().metadata().kid())
        );
    }

    @Test
    @DisplayName("Deve rejeitar chave aposentada com identificador nulo")
    void constructor_chaveAposentadaComKidNulo_deveLancarExcecao() {
        // Arrange
        KeysProperties.RetiringKey retiringKey = createRetiringKey(
                null,
                "conteudo",
                null
        );

        when(properties.getRetiring()).thenReturn(List.of(retiringKey));

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertEquals(
                "Retired signing key is missing required 'kid' for JWKS",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar chave aposentada com identificador em branco")
    void constructor_chaveAposentadaComKidEmBranco_deveLancarExcecao() {
        // Arrange
        KeysProperties.RetiringKey retiringKey = createRetiringKey(
                "   ",
                "conteudo",
                null
        );

        when(properties.getRetiring()).thenReturn(List.of(retiringKey));

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertEquals(
                "Retired signing key is missing required 'kid' for JWKS",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar chave aposentada sem material público configurado")
    void constructor_chaveAposentadaSemMaterialPublico_deveLancarExcecao() {
        // Arrange
        KeysProperties.RetiringKey retiringKey = createRetiringKey(
                RETIRING_KID,
                null,
                " "
        );

        when(properties.getRetiring()).thenReturn(List.of(retiringKey));

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::createProvider
        );

        // Assert
        assertEquals(
                "ECO-20: chave aposentada kid="
                        + RETIRING_KID
                        + " sem material público configurado.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar conteúdo vazio ao converter uma chave privada PEM")
    void parsePrivateKey_pemVazio_deveLancarExcecao() {
        // Arrange
        String pem = """
                -----BEGIN PRIVATE KEY-----

                -----END PRIVATE KEY-----
                """;

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ConfigurableSigningKeyProvider.parsePrivateKey(pem)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "Invalid private key (expected PEM PKCS#8 RSA)",
                        exception.getMessage()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                ),
                () -> assertEquals("PEM vazio", exception.getCause().getMessage())
        );
    }

    @Test
    @DisplayName("Deve rejeitar conteúdo DER inválido ao converter uma chave privada")
    void parsePrivateKey_conteudoDerInvalido_deveLancarExcecao() {
        // Arrange
        String pem = """
                -----BEGIN PRIVATE KEY-----
                AQID
                -----END PRIVATE KEY-----
                """;

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ConfigurableSigningKeyProvider.parsePrivateKey(pem)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "Invalid private key (expected PEM PKCS#8 RSA)",
                        exception.getMessage()
                ),
                () -> assertNotNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve rejeitar conteúdo DER inválido ao converter uma chave pública")
    void parsePublicKey_conteudoDerInvalido_deveLancarExcecao() {
        // Arrange
        String pem = """
                -----BEGIN PUBLIC KEY-----
                AQID
                -----END PUBLIC KEY-----
                """;

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ConfigurableSigningKeyProvider.parsePublicKey(pem)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "Invalid public key (expected PEM X.509 RSA)",
                        exception.getMessage()
                ),
                () -> assertNotNull(exception.getCause())
        );
    }

    @Test
    @DisplayName("Deve converter uma chave privada RSA válida no formato PEM")
    void parsePrivateKey_pemValido_deveRetornarChavePrivadaRsa() throws Exception {
        // Arrange
        KeyPair pair = generateRsaKeyPair();
        String pem = toPrivatePem(pair);

        // Act
        RSAPrivateKey result = ConfigurableSigningKeyProvider.parsePrivateKey(pem);

        // Assert
        assertEquals(
                ((RSAPrivateKey) pair.getPrivate()).getModulus(),
                result.getModulus()
        );
    }

    @Test
    @DisplayName("Deve converter uma chave pública RSA válida no formato PEM")
    void parsePublicKey_pemValido_deveRetornarChavePublicaRsa() throws Exception {
        // Arrange
        KeyPair pair = generateRsaKeyPair();
        String pem = toPublicPem(pair);

        // Act
        RSAPublicKey result = ConfigurableSigningKeyProvider.parsePublicKey(pem);

        // Assert
        assertAll(
                () -> assertEquals(
                        ((RSAPublicKey) pair.getPublic()).getModulus(),
                        result.getModulus()
                ),
                () -> assertEquals(
                        ((RSAPublicKey) pair.getPublic()).getPublicExponent(),
                        result.getPublicExponent()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar chave privada RSA que não forneça parâmetros CRT")
    void derivePublicKey_chavePrivadaSemCrt_deveLancarExcecao() throws Exception {
        // Arrange
        RSAPrivateKey privateKey = mock(RSAPrivateKey.class);
        Method method = privateMethod("derivePublicKey", RSAPrivateKey.class);

        // Act
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, privateKey)
        );

        // Assert
        assertAll(
                () -> assertInstanceOf(
                        IllegalStateException.class,
                        exception.getCause()
                ),
                () -> assertEquals(
                        "ECO-20: chave privada não permite derivar a pública "
                                + "(esperado RSA CRT/PKCS#8).",
                        exception.getCause().getMessage()
                )
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando falhar a derivação da chave pública")
    void derivePublicKey_parametrosCrtInvalidos_deveLancarExcecao() throws Exception {
        // Arrange
        RSAPrivateCrtKey privateKey = mock(RSAPrivateCrtKey.class);
        Method method = privateMethod("derivePublicKey", RSAPrivateKey.class);

        when(privateKey.getModulus()).thenReturn(null);
        when(privateKey.getPublicExponent()).thenReturn(null);

        // Act
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, privateKey)
        );

        // Assert
        assertAll(
                () -> assertInstanceOf(
                        IllegalStateException.class,
                        exception.getCause()
                ),
                () -> assertEquals(
                        "Failed to derive public key from private key",
                        exception.getCause().getMessage()
                ),
                () -> assertNotNull(exception.getCause().getCause())
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando o gerador RSA não estiver disponível")
    void generateKeyPair_algoritmoRsaIndisponivel_deveLancarExcecao() throws Exception {
        // Arrange
        Method method = privateMethod("generateKeyPair");

        try (MockedStatic<KeyPairGenerator> mockedGenerator =
                     mockStatic(KeyPairGenerator.class)) {

            mockedGenerator.when(() -> KeyPairGenerator.getInstance("RSA"))
                    .thenThrow(new NoSuchAlgorithmException("RSA indisponível"));

            // Act
            InvocationTargetException exception = assertThrows(
                    InvocationTargetException.class,
                    () -> method.invoke(null)
            );

            // Assert
            assertAll(
                    () -> assertInstanceOf(
                            IllegalStateException.class,
                            exception.getCause()
                    ),
                    () -> assertEquals(
                            "Failed to generate in-memory RSA key pair",
                            exception.getCause().getMessage()
                    ),
                    () -> assertInstanceOf(
                            NoSuchAlgorithmException.class,
                            exception.getCause().getCause()
                    )
            );
        }
    }

    private ConfigurableSigningKeyProvider createProvider() {
        return new ConfigurableSigningKeyProvider(
                properties,
                resourceLoader,
                clock
        );
    }

    private KeysProperties.RetiringKey createRetiringKey(
            String kid,
            String publicKey,
            String publicKeyLocation
    ) {
        KeysProperties.RetiringKey retiringKey =
                mock(KeysProperties.RetiringKey.class);

        when(retiringKey.getKid()).thenReturn(kid);
        when(retiringKey.getPublicKey()).thenReturn(publicKey);
        when(retiringKey.getPublicKeyLocation()).thenReturn(publicKeyLocation);

        return retiringKey;
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String toPrivatePem(KeyPair pair) {
        return toPem("PRIVATE KEY", pair.getPrivate().getEncoded());
    }

    private static String toPublicPem(KeyPair pair) {
        return toPem("PUBLIC KEY", pair.getPublic().getEncoded());
    }

    private static String toPem(String type, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(
                64,
                System.lineSeparator().getBytes(StandardCharsets.UTF_8)
        ).encodeToString(encoded);

        return "-----BEGIN " + type + "-----"
                + System.lineSeparator()
                + base64
                + System.lineSeparator()
                + "-----END " + type + "-----";
    }

    private static Method privateMethod(
            String name,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        Method method = ConfigurableSigningKeyProvider.class.getDeclaredMethod(
                name,
                parameterTypes
        );
        method.setAccessible(true);
        return method;
    }
}
