package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários da aplicação cliente OAuth2")
class ClientApplicationTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-07-22T10:00:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-07-22T11:00:00Z");

    @Test
    @DisplayName("Deve reconstruir uma aplicação cliente preservando e normalizando os dados")
    void constructor_dadosValidos_deveReconstruirAplicacaoCliente() {
        // Arrange
        Set<GrantType> grantTypes = Set.of(
                GrantType.PASSWORD,
                GrantType.REFRESH_TOKEN
        );

        Set<String> redirectUris = new HashSet<>(Arrays.asList(
                " https://ecofy.com/callback ",
                "https://ecofy.com/callback",
                "   ",
                null
        ));

        Set<String> scopes = new HashSet<>(Arrays.asList(
                " openid ",
                "openid",
                " profile ",
                "",
                null
        ));

        // Act
        ClientApplication application = new ClientApplication(
                "internal-id",
                "ecofy-client",
                "secret-hash",
                "EcoFy",
                ClientType.CONFIDENTIAL,
                grantTypes,
                redirectUris,
                scopes,
                true,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        // Assert
        assertAll(
                () -> assertEquals("internal-id", application.id()),
                () -> assertEquals(
                        "ecofy-client",
                        application.clientId()
                ),
                () -> assertEquals(
                        "secret-hash",
                        application.clientSecretHash()
                ),
                () -> assertEquals("EcoFy", application.name()),
                () -> assertEquals(
                        ClientType.CONFIDENTIAL,
                        application.clientType()
                ),
                () -> assertEquals(
                        grantTypes,
                        application.grantTypes()
                ),
                () -> assertEquals(
                        Set.of("https://ecofy.com/callback"),
                        application.redirectUris()
                ),
                () -> assertEquals(
                        Set.of("openid", "profile"),
                        application.scopes()
                ),
                () -> assertTrue(application.isFirstParty()),
                () -> assertTrue(application.isActive()),
                () -> assertEquals(
                        CREATED_AT,
                        application.createdAt()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        application.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar segredo nulo para uma aplicação cliente não confidencial")
    void constructor_clienteNaoConfidencialComSegredoNulo_deveCriarAplicacao() {
        // Arrange e Act
        ClientApplication application = new ClientApplication(
                "internal-id",
                "ecofy-spa",
                null,
                "EcoFy SPA",
                ClientType.SPA,
                Set.of(GrantType.PASSWORD),
                Set.of(),
                Set.of(),
                false,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        // Assert
        assertNull(application.clientSecretHash());
    }

    @Test
    @DisplayName("Deve criar coleções vazias quando elas forem nulas")
    void constructor_colecoesNulas_deveCriarColecoesVazias() {
        // Arrange e Act
        ClientApplication application = new ClientApplication(
                "internal-id",
                "ecofy-spa",
                null,
                "EcoFy SPA",
                ClientType.SPA,
                null,
                null,
                null,
                false,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        // Assert
        assertAll(
                () -> assertTrue(application.grantTypes().isEmpty()),
                () -> assertTrue(application.redirectUris().isEmpty()),
                () -> assertTrue(application.scopes().isEmpty())
        );
    }

    @Test
    @DisplayName("Deve preservar coleções vazias recebidas na construção")
    void constructor_colecoesVazias_deveManterColecoesVazias() {
        // Arrange e Act
        ClientApplication application = new ClientApplication(
                "internal-id",
                "ecofy-spa",
                null,
                "EcoFy SPA",
                ClientType.SPA,
                Set.of(),
                Set.of(),
                Set.of(),
                false,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        // Assert
        assertAll(
                () -> assertTrue(application.grantTypes().isEmpty()),
                () -> assertTrue(application.redirectUris().isEmpty()),
                () -> assertTrue(application.scopes().isEmpty())
        );
    }

    @Test
    @DisplayName("Deve copiar as coleções recebidas e proteger o estado interno")
    void constructor_colecoesMutaveis_deveRealizarCopiasDefensivas() {
        // Arrange
        Set<GrantType> grantTypes = new HashSet<>(
                Set.of(GrantType.PASSWORD)
        );
        Set<String> redirectUris = new HashSet<>(
                Set.of("https://ecofy.com/callback")
        );
        Set<String> scopes = new HashSet<>(Set.of("openid"));

        ClientApplication application = new ClientApplication(
                "internal-id",
                "ecofy-spa",
                null,
                "EcoFy SPA",
                ClientType.SPA,
                grantTypes,
                redirectUris,
                scopes,
                false,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        // Act
        grantTypes.add(GrantType.REFRESH_TOKEN);
        redirectUris.add("https://ecofy.com/other");
        scopes.add("profile");

        // Assert
        assertAll(
                () -> assertEquals(
                        Set.of(GrantType.PASSWORD),
                        application.grantTypes()
                ),
                () -> assertEquals(
                        Set.of("https://ecofy.com/callback"),
                        application.redirectUris()
                ),
                () -> assertEquals(
                        Set.of("openid"),
                        application.scopes()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> application.grantTypes()
                                .add(GrantType.REFRESH_TOKEN)
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> application.redirectUris()
                                .add("https://ecofy.com/other")
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> application.scopes().add("profile")
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar um identificador interno nulo")
    void constructor_idNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        null,
                        "ecofy-client",
                        null,
                        "EcoFy",
                        ClientType.SPA,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertEquals(
                "id must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um identificador público nulo")
    void constructor_clientIdNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        null,
                        null,
                        "EcoFy",
                        ClientType.SPA,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertEquals(
                "clientId must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um nome nulo")
    void constructor_nomeNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "ecofy-client",
                        null,
                        null,
                        ClientType.SPA,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertEquals(
                "name must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar um tipo de cliente nulo")
    void constructor_clientTypeNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "ecofy-client",
                        null,
                        "EcoFy",
                        null,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertEquals(
                "clientType must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar uma data de criação nula")
    void constructor_createdAtNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "ecofy-client",
                        null,
                        "EcoFy",
                        ClientType.SPA,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        true,
                        null,
                        UPDATED_AT
                )
        );

        assertEquals(
                "createdAt must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar uma data de atualização nula")
    void constructor_updatedAtNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "ecofy-client",
                        null,
                        "EcoFy",
                        ClientType.SPA,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        true,
                        CREATED_AT,
                        null
                )
        );

        assertEquals(
                "updatedAt must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar segredo nulo para uma aplicação cliente confidencial")
    void constructor_clienteConfidencialComSegredoNulo_deveLancarIllegalArgumentException() {
        // Arrange, Act e Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "ecofy-client",
                        null,
                        "EcoFy",
                        ClientType.CONFIDENTIAL,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        true,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertEquals(
                "clientSecretHash must be provided for CONFIDENTIAL clients",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar segredo em branco para uma aplicação cliente confidencial")
    void constructor_clienteConfidencialComSegredoEmBranco_deveLancarIllegalArgumentException() {
        // Arrange, Act e Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "ecofy-client",
                        "   ",
                        "EcoFy",
                        ClientType.CONFIDENTIAL,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        true,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertEquals(
                "clientSecretHash must be provided for CONFIDENTIAL clients",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve criar uma aplicação cliente ativa com identificadores e datas gerados")
    void create_dadosValidos_deveCriarAplicacaoAtiva() {
        // Arrange
        Instant beforeCreation = Instant.now();

        // Act
        ClientApplication application = ClientApplication.create(
                "EcoFy SPA",
                ClientType.SPA,
                Set.of(GrantType.PASSWORD),
                Set.of("https://ecofy.com/callback"),
                Set.of("openid"),
                true,
                "generated-client-id",
                null
        );

        Instant afterCreation = Instant.now();

        // Assert
        assertAll(
                () -> assertNotNull(application.id()),
                () -> assertFalse(application.id().isBlank()),
                () -> assertNotNull(
                        UUID.fromString(application.id())
                ),
                () -> assertEquals(
                        "generated-client-id",
                        application.clientId()
                ),
                () -> assertEquals("EcoFy SPA", application.name()),
                () -> assertEquals(
                        ClientType.SPA,
                        application.clientType()
                ),
                () -> assertNull(application.clientSecretHash()),
                () -> assertEquals(
                        Set.of(GrantType.PASSWORD),
                        application.grantTypes()
                ),
                () -> assertEquals(
                        Set.of("https://ecofy.com/callback"),
                        application.redirectUris()
                ),
                () -> assertEquals(
                        Set.of("openid"),
                        application.scopes()
                ),
                () -> assertTrue(application.isFirstParty()),
                () -> assertTrue(application.isActive()),
                () -> assertEquals(
                        application.createdAt(),
                        application.updatedAt()
                ),
                () -> assertFalse(
                        application.createdAt()
                                .isBefore(beforeCreation)
                ),
                () -> assertFalse(
                        application.createdAt()
                                .isAfter(afterCreation)
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar a criação quando o identificador público gerado for nulo")
    void create_generatedClientIdNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ClientApplication.create(
                        "EcoFy SPA",
                        ClientType.SPA,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        false,
                        null,
                        null
                )
        );

        assertEquals(
                "generatedClientId must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve indicar suporte quando o grant type estiver configurado")
    void supportsGrant_grantTypeConfigurado_deveRetornarTrue() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(GrantType.PASSWORD),
                Set.of(),
                Set.of()
        );

        // Act
        boolean result = application.supportsGrant(
                GrantType.PASSWORD
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve negar suporte quando o grant type não estiver configurado")
    void supportsGrant_grantTypeNaoConfigurado_deveRetornarFalse() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(GrantType.PASSWORD),
                Set.of(),
                Set.of()
        );

        // Act
        boolean result = application.supportsGrant(
                GrantType.REFRESH_TOKEN
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve rejeitar a consulta de um grant type nulo")
    void supportsGrant_grantTypeNulo_deveLancarNullPointerException() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of()
        );

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> application.supportsGrant(null)
        );

        // Assert
        assertEquals(
                "grantType must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve reconhecer uma URI de redirecionamento autorizada após remover espaços")
    void supportsRedirectUri_uriAutorizadaComEspacos_deveRetornarTrue() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of("https://ecofy.com/callback"),
                Set.of()
        );

        // Act
        boolean result = application.supportsRedirectUri(
                "  https://ecofy.com/callback  "
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve negar uma URI de redirecionamento não autorizada")
    void supportsRedirectUri_uriNaoAutorizada_deveRetornarFalse() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of("https://ecofy.com/callback"),
                Set.of()
        );

        // Act
        boolean result = application.supportsRedirectUri(
                "https://ecofy.com/other"
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve negar a URI quando nenhuma URI de redirecionamento estiver configurada")
    void supportsRedirectUri_semUrisConfiguradas_deveRetornarFalse() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of()
        );

        // Act
        boolean result = application.supportsRedirectUri(
                "https://ecofy.com/callback"
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve rejeitar uma URI de redirecionamento nula")
    void supportsRedirectUri_uriNula_deveLancarNullPointerException() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of()
        );

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> application.supportsRedirectUri(null)
        );

        // Assert
        assertEquals(
                "redirectUri must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve reconhecer uma scope autorizada após remover espaços")
    void supportsScope_scopeAutorizadaComEspacos_deveRetornarTrue() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of("openid")
        );

        // Act
        boolean result = application.supportsScope("  openid  ");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve negar uma scope não autorizada")
    void supportsScope_scopeNaoAutorizada_deveRetornarFalse() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of("openid")
        );

        // Act
        boolean result = application.supportsScope("profile");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve negar a scope quando nenhuma scope estiver configurada")
    void supportsScope_semScopesConfiguradas_deveRetornarFalse() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of()
        );

        // Act
        boolean result = application.supportsScope("openid");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve rejeitar uma scope nula")
    void supportsScope_scopeNula_deveLancarNullPointerException() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of()
        );

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> application.supportsScope(null)
        );

        // Assert
        assertEquals(
                "requestedScope must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve aceitar a ausência de scopes solicitadas")
    void supportsAllScopes_scopesSolicitadasNulas_deveRetornarTrue() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of()
        );

        // Act
        boolean result = application.supportsAllScopes(null);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve aceitar um conjunto vazio de scopes solicitadas")
    void supportsAllScopes_scopesSolicitadasVazias_deveRetornarTrue() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of()
        );

        // Act
        boolean result = application.supportsAllScopes(Set.of());

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve negar scopes solicitadas quando nenhuma scope estiver configurada")
    void supportsAllScopes_semScopesConfiguradas_deveRetornarFalse() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of()
        );

        // Act
        boolean result = application.supportsAllScopes(
                Set.of("openid")
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve aceitar todas as scopes autorizadas após normalizar os valores")
    void supportsAllScopes_todasScopesAutorizadas_deveRetornarTrue() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of("openid", "profile")
        );

        // Act
        boolean result = application.supportsAllScopes(
                Set.of(" openid ", " profile ")
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve ignorar valores nulos ao validar todas as scopes solicitadas")
    void supportsAllScopes_scopeNulaEAutorizada_deveRetornarTrue() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of("openid")
        );

        Set<String> requestedScopes = new HashSet<>(
                Arrays.asList(null, " openid ")
        );

        // Act
        boolean result = application.supportsAllScopes(
                requestedScopes
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve aceitar um conjunto contendo somente uma scope nula")
    void supportsAllScopes_apenasScopeNula_deveRetornarTrue() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of("openid")
        );

        Set<String> requestedScopes = new HashSet<>();
        requestedScopes.add(null);

        // Act
        boolean result = application.supportsAllScopes(
                requestedScopes
        );

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve negar quando ao menos uma scope solicitada não estiver autorizada")
    void supportsAllScopes_scopeNaoAutorizada_deveRetornarFalse() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of("openid")
        );

        // Act
        boolean result = application.supportsAllScopes(
                Set.of("openid", "profile")
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve rotacionar o segredo de uma aplicação cliente confidencial")
    void rotateSecret_clienteConfidencial_deveAtualizarSegredoETimestamp() {
        // Arrange
        ClientApplication application = createConfidentialApplication(
                "old-secret-hash",
                true
        );

        // Act
        application.rotateSecret("new-secret-hash");

        // Assert
        assertAll(
                () -> assertEquals(
                        "new-secret-hash",
                        application.clientSecretHash()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        application.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar segredo em branco durante a rotação conforme o comportamento atual")
    void rotateSecret_hashEmBranco_deveAtualizarSegredo() {
        // Arrange
        ClientApplication application = createConfidentialApplication(
                "old-secret-hash",
                true
        );

        // Act
        application.rotateSecret("   ");

        // Assert
        assertAll(
                () -> assertEquals(
                        "   ",
                        application.clientSecretHash()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        application.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar a rotação para um segredo nulo")
    void rotateSecret_novoSegredoNulo_deveLancarNullPointerException() {
        // Arrange
        ClientApplication application = createConfidentialApplication(
                "old-secret-hash",
                true
        );

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> application.rotateSecret(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "newSecretHash must not be null",
                        exception.getMessage()
                ),
                () -> assertEquals(
                        "old-secret-hash",
                        application.clientSecretHash()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        application.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar a rotação de segredo para uma aplicação não confidencial")
    void rotateSecret_clienteNaoConfidencial_deveLancarIllegalStateException() {
        // Arrange
        ClientApplication application = createApplication(
                Set.of(),
                Set.of(),
                Set.of()
        );

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> application.rotateSecret("new-secret-hash")
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "Only CONFIDENTIAL clients can have a secret",
                        exception.getMessage()
                ),
                () -> assertNull(application.clientSecretHash()),
                () -> assertEquals(
                        UPDATED_AT,
                        application.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve desativar uma aplicação ativa e atualizar o timestamp")
    void deactivate_aplicacaoAtiva_deveDesativarAplicacao() {
        // Arrange
        ClientApplication application = createApplication(true);

        // Act
        application.deactivate();

        // Assert
        assertAll(
                () -> assertFalse(application.isActive()),
                () -> assertNotEquals(
                        UPDATED_AT,
                        application.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve manter o estado e o timestamp ao desativar uma aplicação já inativa")
    void deactivate_aplicacaoInativa_deveManterEstado() {
        // Arrange
        ClientApplication application = createApplication(false);

        // Act
        application.deactivate();

        // Assert
        assertAll(
                () -> assertFalse(application.isActive()),
                () -> assertEquals(
                        UPDATED_AT,
                        application.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve ativar uma aplicação inativa e atualizar o timestamp")
    void activate_aplicacaoInativa_deveAtivarAplicacao() {
        // Arrange
        ClientApplication application = createApplication(false);

        // Act
        application.activate();

        // Assert
        assertAll(
                () -> assertTrue(application.isActive()),
                () -> assertNotEquals(
                        UPDATED_AT,
                        application.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve manter o estado e o timestamp ao ativar uma aplicação já ativa")
    void activate_aplicacaoAtiva_deveManterEstado() {
        // Arrange
        ClientApplication application = createApplication(true);

        // Act
        application.activate();

        // Assert
        assertAll(
                () -> assertTrue(application.isActive()),
                () -> assertEquals(
                        UPDATED_AT,
                        application.updatedAt()
                )
        );
    }

    private ClientApplication createApplication(
            Set<GrantType> grantTypes,
            Set<String> redirectUris,
            Set<String> scopes
    ) {
        return new ClientApplication(
                "internal-id",
                "ecofy-spa",
                null,
                "EcoFy SPA",
                ClientType.SPA,
                grantTypes,
                redirectUris,
                scopes,
                false,
                true,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private ClientApplication createApplication(boolean active) {
        return new ClientApplication(
                "internal-id",
                "ecofy-spa",
                null,
                "EcoFy SPA",
                ClientType.SPA,
                Set.of(),
                Set.of(),
                Set.of(),
                false,
                active,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private ClientApplication createConfidentialApplication(
            String clientSecretHash,
            boolean active
    ) {
        return new ClientApplication(
                "internal-id",
                "ecofy-client",
                clientSecretHash,
                "EcoFy",
                ClientType.CONFIDENTIAL,
                Set.of(GrantType.PASSWORD),
                Set.of(),
                Set.of(),
                true,
                active,
                CREATED_AT,
                UPDATED_AT
        );
    }
}
