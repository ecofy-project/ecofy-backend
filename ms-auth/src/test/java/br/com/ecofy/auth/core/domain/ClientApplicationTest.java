package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClientApplicationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T10:00:00Z");

    @Test
    void shouldCreateClientApplicationWithAllFields() {
        GrantType grantType = anyGrantType();

        Set<String> redirectUris = new HashSet<>(Arrays.asList(
                " https://app.ecofy.com/callback ",
                null,
                "   "
        ));

        Set<String> scopes = new HashSet<>(Arrays.asList(
                " openid ",
                "profile",
                null,
                "   "
        ));

        ClientApplication client = new ClientApplication(
                "internal-id",
                "client-id",
                "secret-hash",
                "EcoFy Web",
                ClientType.CONFIDENTIAL,
                Set.of(grantType),
                redirectUris,
                scopes,
                true,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        assertEquals("internal-id", client.id());
        assertEquals("client-id", client.clientId());
        assertEquals("secret-hash", client.clientSecretHash());
        assertEquals("EcoFy Web", client.name());
        assertEquals(ClientType.CONFIDENTIAL, client.clientType());

        assertTrue(client.grantTypes().contains(grantType));

        assertTrue(client.redirectUris().contains("https://app.ecofy.com/callback"));
        assertFalse(client.redirectUris().contains(" https://app.ecofy.com/callback "));
        assertFalse(client.redirectUris().contains("   "));
        assertFalse(client.redirectUris().contains(null));

        assertEquals(Set.of("openid", "profile"), client.scopes());
        assertFalse(client.scopes().contains(" openid "));
        assertFalse(client.scopes().contains("   "));
        assertFalse(client.scopes().contains(null));

        assertTrue(client.isFirstParty());
        assertTrue(client.isActive());
        assertEquals(CREATED_AT, client.createdAt());
        assertEquals(UPDATED_AT, client.updatedAt());
    }

    @Test
    void shouldCreateClientApplicationUsingFactory() {
        GrantType grantType = anyGrantType();

        ClientApplication client = ClientApplication.create(
                "EcoFy Mobile",
                ClientType.CONFIDENTIAL,
                Set.of(grantType),
                Set.of("https://mobile.ecofy.com/callback"),
                Set.of("openid"),
                false,
                "generated-client-id",
                "generated-secret-hash"
        );

        assertNotNull(client.id());
        assertEquals("generated-client-id", client.clientId());
        assertEquals("generated-secret-hash", client.clientSecretHash());
        assertEquals("EcoFy Mobile", client.name());
        assertEquals(ClientType.CONFIDENTIAL, client.clientType());
        assertTrue(client.grantTypes().contains(grantType));
        assertEquals(Set.of("https://mobile.ecofy.com/callback"), client.redirectUris());
        assertEquals(Set.of("openid"), client.scopes());
        assertFalse(client.isFirstParty());
        assertTrue(client.isActive());
        assertNotNull(client.createdAt());
        assertNotNull(client.updatedAt());
    }

    @Test
    void shouldThrowExceptionWhenGeneratedClientIdIsNullInFactory() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ClientApplication.create(
                        "EcoFy",
                        ClientType.CONFIDENTIAL,
                        Set.of(anyGrantType()),
                        Set.of("https://app.ecofy.com/callback"),
                        Set.of("openid"),
                        true,
                        null,
                        "secret-hash"
                )
        );

        assertEquals("generatedClientId must not be null", exception.getMessage());
    }

    @Test
    void shouldInitializeEmptyCollectionsWhenSetsAreNull() {
        ClientApplication client = new ClientApplication(
                "internal-id",
                "client-id",
                null,
                "EcoFy Public",
                nonConfidentialClientType(),
                null,
                null,
                null,
                false,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        assertTrue(client.grantTypes().isEmpty());
        assertTrue(client.redirectUris().isEmpty());
        assertTrue(client.scopes().isEmpty());
    }

    @Test
    void shouldInitializeEmptyCollectionsWhenUriAndScopeSetsAreEmpty() {
        ClientApplication client = new ClientApplication(
                "internal-id",
                "client-id",
                null,
                "EcoFy Public",
                nonConfidentialClientType(),
                Set.of(),
                Set.of(),
                Set.of(),
                false,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        assertTrue(client.grantTypes().isEmpty());
        assertTrue(client.redirectUris().isEmpty());
        assertTrue(client.scopes().isEmpty());
    }

    @Test
    void shouldProtectInternalCollectionsFromExternalMutation() {
        GrantType grantType = anyGrantType();

        Set<GrantType> grantTypes = new HashSet<>();
        grantTypes.add(grantType);

        Set<String> redirectUris = new HashSet<>();
        redirectUris.add("https://app.ecofy.com/callback");

        Set<String> scopes = new HashSet<>();
        scopes.add("openid");

        ClientApplication client = new ClientApplication(
                "internal-id",
                "client-id",
                null,
                "EcoFy Public",
                nonConfidentialClientType(),
                grantTypes,
                redirectUris,
                scopes,
                false,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        grantTypes.clear();
        redirectUris.clear();
        scopes.clear();

        assertTrue(client.grantTypes().contains(grantType));
        assertTrue(client.redirectUris().contains("https://app.ecofy.com/callback"));
        assertTrue(client.scopes().contains("openid"));
    }

    @Test
    void shouldReturnUnmodifiableCollections() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        assertThrows(UnsupportedOperationException.class, () -> client.grantTypes().add(anyGrantType()));
        assertThrows(UnsupportedOperationException.class, () -> client.redirectUris().add("https://evil.com/callback"));
        assertThrows(UnsupportedOperationException.class, () -> client.scopes().add("admin"));
    }

    @Test
    void shouldThrowExceptionWhenRequiredConstructorArgumentsAreNull() {
        assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        null,
                        "client-id",
                        null,
                        "EcoFy",
                        nonConfidentialClientType(),
                        null,
                        null,
                        null,
                        false,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        null,
                        null,
                        "EcoFy",
                        nonConfidentialClientType(),
                        null,
                        null,
                        null,
                        false,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "client-id",
                        null,
                        null,
                        nonConfidentialClientType(),
                        null,
                        null,
                        null,
                        false,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "client-id",
                        null,
                        "EcoFy",
                        null,
                        null,
                        null,
                        null,
                        false,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "client-id",
                        null,
                        "EcoFy",
                        nonConfidentialClientType(),
                        null,
                        null,
                        null,
                        false,
                        true,
                        null,
                        UPDATED_AT
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "client-id",
                        null,
                        "EcoFy",
                        nonConfidentialClientType(),
                        null,
                        null,
                        null,
                        false,
                        true,
                        CREATED_AT,
                        null
                )
        );
    }

    @Test
    void shouldRequireClientSecretHashForConfidentialClientWhenNullOrBlank() {
        IllegalArgumentException nullSecretException = assertThrows(
                IllegalArgumentException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "client-id",
                        null,
                        "EcoFy Confidential",
                        ClientType.CONFIDENTIAL,
                        Set.of(anyGrantType()),
                        Set.of("https://app.ecofy.com/callback"),
                        Set.of("openid"),
                        true,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        IllegalArgumentException blankSecretException = assertThrows(
                IllegalArgumentException.class,
                () -> new ClientApplication(
                        "internal-id",
                        "client-id",
                        "   ",
                        "EcoFy Confidential",
                        ClientType.CONFIDENTIAL,
                        Set.of(anyGrantType()),
                        Set.of("https://app.ecofy.com/callback"),
                        Set.of("openid"),
                        true,
                        true,
                        CREATED_AT,
                        UPDATED_AT
                )
        );

        assertEquals(
                "clientSecretHash must be provided for CONFIDENTIAL clients",
                nullSecretException.getMessage()
        );
        assertEquals(
                "clientSecretHash must be provided for CONFIDENTIAL clients",
                blankSecretException.getMessage()
        );
    }

    @Test
    void shouldSupportGrantWhenGrantTypeExists() {
        GrantType grantType = anyGrantType();

        ClientApplication client = basePublicClient(
                Set.of(grantType),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        assertTrue(client.supportsGrant(grantType));
    }

    @Test
    void shouldNotSupportGrantWhenGrantTypeDoesNotExist() {
        GrantType[] grantTypes = GrantType.values();

        if (grantTypes.length < 2) {
            return;
        }

        ClientApplication client = basePublicClient(
                Set.of(grantTypes[0]),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        assertFalse(client.supportsGrant(grantTypes[1]));
    }

    @Test
    void shouldThrowExceptionWhenGrantTypeIsNull() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> client.supportsGrant(null)
        );

        assertEquals("grantType must not be null", exception.getMessage());
    }

    @Test
    void shouldSupportRedirectUriWhenUriExistsAfterTrim() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of(" https://app.ecofy.com/callback "),
                Set.of("openid"),
                true
        );

        assertTrue(client.supportsRedirectUri("   https://app.ecofy.com/callback   "));
    }

    @Test
    void shouldNotSupportRedirectUriWhenUrisAreEmptyOrUriDoesNotExist() {
        ClientApplication clientWithEmptyUris = basePublicClient(
                Set.of(anyGrantType()),
                Set.of(),
                Set.of("openid"),
                true
        );

        ClientApplication clientWithUris = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        assertFalse(clientWithEmptyUris.supportsRedirectUri("https://app.ecofy.com/callback"));
        assertFalse(clientWithUris.supportsRedirectUri("https://unknown.ecofy.com/callback"));
    }

    @Test
    void shouldThrowExceptionWhenRedirectUriIsNull() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> client.supportsRedirectUri(null)
        );

        assertEquals("redirectUri must not be null", exception.getMessage());
    }

    @Test
    void shouldSupportScopeWhenScopeExistsAfterTrim() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of(" openid "),
                true
        );

        assertTrue(client.supportsScope("   openid   "));
    }

    @Test
    void shouldNotSupportScopeWhenScopesAreEmptyOrScopeDoesNotExist() {
        ClientApplication clientWithEmptyScopes = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of(),
                true
        );

        ClientApplication clientWithScopes = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        assertFalse(clientWithEmptyScopes.supportsScope("openid"));
        assertFalse(clientWithScopes.supportsScope("admin"));
    }

    @Test
    void shouldThrowExceptionWhenRequestedScopeIsNull() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> client.supportsScope(null)
        );

        assertEquals("requestedScope must not be null", exception.getMessage());
    }

    @Test
    void shouldSupportAllScopesWhenRequestedScopesAreNullOrEmpty() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        assertTrue(client.supportsAllScopes(null));
        assertTrue(client.supportsAllScopes(Set.of()));
    }

    @Test
    void shouldNotSupportAllScopesWhenAllowedScopesAreEmpty() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of(),
                true
        );

        assertFalse(client.supportsAllScopes(Set.of("openid")));
    }

    @Test
    void shouldSupportAllScopesWhenEveryRequestedScopeIsAllowedAfterTrimAndNullFiltering() {
        Set<String> requestedScopes = new HashSet<>();
        requestedScopes.add(" openid ");
        requestedScopes.add("profile");
        requestedScopes.add(null);

        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid", "profile"),
                true
        );

        assertTrue(client.supportsAllScopes(requestedScopes));
    }

    @Test
    void shouldNotSupportAllScopesWhenAnyRequestedScopeIsNotAllowed() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid", "profile"),
                true
        );

        assertFalse(client.supportsAllScopes(Set.of("openid", "admin")));
    }

    @Test
    void shouldRotateSecretForConfidentialClient() {
        ClientApplication client = new ClientApplication(
                "internal-id",
                "client-id",
                "old-secret-hash",
                "EcoFy Confidential",
                ClientType.CONFIDENTIAL,
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        client.rotateSecret("new-secret-hash");

        assertEquals("new-secret-hash", client.clientSecretHash());
        assertTrue(client.updatedAt().isAfter(UPDATED_AT));
    }

    @Test
    void shouldThrowExceptionWhenRotatingSecretForNonConfidentialClient() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.rotateSecret("new-secret-hash")
        );

        assertEquals("Only CONFIDENTIAL clients can have a secret", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRotatingSecretToNull() {
        ClientApplication client = new ClientApplication(
                "internal-id",
                "client-id",
                "old-secret-hash",
                "EcoFy Confidential",
                ClientType.CONFIDENTIAL,
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> client.rotateSecret(null)
        );

        assertEquals("newSecretHash must not be null", exception.getMessage());
    }

    @Test
    void shouldDeactivateActiveClient() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        client.deactivate();

        assertFalse(client.isActive());
        assertTrue(client.updatedAt().isAfter(UPDATED_AT));
    }

    @Test
    void shouldDoNothingWhenClientIsAlreadyInactive() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                false
        );

        client.deactivate();

        assertFalse(client.isActive());
        assertEquals(UPDATED_AT, client.updatedAt());
    }

    @Test
    void shouldActivateInactiveClient() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                false
        );

        client.activate();

        assertTrue(client.isActive());
        assertTrue(client.updatedAt().isAfter(UPDATED_AT));
    }

    @Test
    void shouldDoNothingWhenClientIsAlreadyActive() {
        ClientApplication client = basePublicClient(
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true
        );

        client.activate();

        assertTrue(client.isActive());
        assertEquals(UPDATED_AT, client.updatedAt());
    }

    private ClientApplication basePublicClient(Set<GrantType> grantTypes,
                                               Set<String> redirectUris,
                                               Set<String> scopes,
                                               boolean active) {
        return new ClientApplication(
                "internal-id",
                "client-id",
                null,
                "EcoFy Public",
                nonConfidentialClientType(),
                grantTypes,
                redirectUris,
                scopes,
                false,
                active,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static GrantType anyGrantType() {
        return Arrays.stream(GrantType.values())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("GrantType enum must have at least one value"));
    }

    private static ClientType nonConfidentialClientType() {
        return Arrays.stream(ClientType.values())
                .filter(clientType -> clientType != ClientType.CONFIDENTIAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ClientType enum must have at least one non-CONFIDENTIAL value"));
    }
}