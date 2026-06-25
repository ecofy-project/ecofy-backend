package br.com.ecofy.auth.adapters.in.web.mapper;

import br.com.ecofy.auth.adapters.in.web.dto.response.ClientApplicationResponse;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientApplicationMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T10:00:00Z");

    @Test
    void shouldMapClientApplicationToResponse() {
        GrantType grantType = anyGrantType();

        ClientApplication client = new ClientApplication(
                "internal-id",
                "ecofy-web",
                null,
                "EcoFy Web",
                nonConfidentialClientType(),
                Set.of(grantType),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid", "profile"),
                true,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        assertEquals("internal-id", response.id());
        assertEquals("ecofy-web", response.clientId());
        assertEquals("EcoFy Web", response.name());
        assertEquals(nonConfidentialClientType(), response.clientType());
        assertEquals(Set.of(grantType), response.grantTypes());
        assertEquals(Set.of("https://app.ecofy.com/callback"), response.redirectUris());
        assertEquals(Set.of("openid", "profile"), response.scopes());
        assertTrue(response.firstParty());
        assertTrue(response.active());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(UPDATED_AT, response.updatedAt());
    }

    @Test
    void shouldGenerateResponseIdWhenClientIdIsNullInDomainObject() {
        GrantType grantType = anyGrantType();

        ClientApplication client = mock(ClientApplication.class);

        when(client.id()).thenReturn(null);
        when(client.clientId()).thenReturn("ecofy-web");
        when(client.name()).thenReturn("EcoFy Web");
        when(client.clientType()).thenReturn(nonConfidentialClientType());
        when(client.grantTypes()).thenReturn(Set.of(grantType));
        when(client.redirectUris()).thenReturn(Set.of("https://app.ecofy.com/callback"));
        when(client.scopes()).thenReturn(Set.of("openid"));
        when(client.isFirstParty()).thenReturn(false);
        when(client.isActive()).thenReturn(true);
        when(client.createdAt()).thenReturn(CREATED_AT);
        when(client.updatedAt()).thenReturn(UPDATED_AT);

        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        assertNotNull(response.id());
        assertFalse(response.id().isBlank());
        assertDoesNotThrow(() -> java.util.UUID.fromString(response.id()));

        assertEquals("ecofy-web", response.clientId());
        assertEquals("EcoFy Web", response.name());
        assertEquals(nonConfidentialClientType(), response.clientType());
        assertEquals(Set.of(grantType), response.grantTypes());
        assertEquals(Set.of("https://app.ecofy.com/callback"), response.redirectUris());
        assertEquals(Set.of("openid"), response.scopes());
        assertFalse(response.firstParty());
        assertTrue(response.active());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(UPDATED_AT, response.updatedAt());
    }

    @Test
    void shouldReturnEmptySetsWhenClientCollectionsAreNull() {
        ClientApplication client = mock(ClientApplication.class);

        when(client.id()).thenReturn("internal-id");
        when(client.clientId()).thenReturn("ecofy-web");
        when(client.name()).thenReturn("EcoFy Web");
        when(client.clientType()).thenReturn(nonConfidentialClientType());
        when(client.grantTypes()).thenReturn(null);
        when(client.redirectUris()).thenReturn(null);
        when(client.scopes()).thenReturn(null);
        when(client.isFirstParty()).thenReturn(false);
        when(client.isActive()).thenReturn(false);
        when(client.createdAt()).thenReturn(CREATED_AT);
        when(client.updatedAt()).thenReturn(UPDATED_AT);

        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        assertTrue(response.grantTypes().isEmpty());
        assertTrue(response.redirectUris().isEmpty());
        assertTrue(response.scopes().isEmpty());
        assertFalse(response.firstParty());
        assertFalse(response.active());
    }

    @Test
    void shouldReturnEmptySetsWhenClientCollectionsAreEmpty() {
        ClientApplication client = mock(ClientApplication.class);

        when(client.id()).thenReturn("internal-id");
        when(client.clientId()).thenReturn("ecofy-web");
        when(client.name()).thenReturn("EcoFy Web");
        when(client.clientType()).thenReturn(nonConfidentialClientType());
        when(client.grantTypes()).thenReturn(Set.of());
        when(client.redirectUris()).thenReturn(Set.of());
        when(client.scopes()).thenReturn(Set.of());
        when(client.isFirstParty()).thenReturn(true);
        when(client.isActive()).thenReturn(true);
        when(client.createdAt()).thenReturn(CREATED_AT);
        when(client.updatedAt()).thenReturn(UPDATED_AT);

        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        assertTrue(response.grantTypes().isEmpty());
        assertTrue(response.redirectUris().isEmpty());
        assertTrue(response.scopes().isEmpty());
    }

    @Test
    void shouldFilterNullValuesFromCollections() {
        GrantType grantType = anyGrantType();

        Set<GrantType> grantTypes = new HashSet<>(Arrays.asList(grantType, null));

        Set<String> redirectUris = new HashSet<>(Arrays.asList(
                "https://app.ecofy.com/callback",
                null
        ));

        Set<String> scopes = new HashSet<>(Arrays.asList(
                "openid",
                "profile",
                null
        ));

        ClientApplication client = mock(ClientApplication.class);

        when(client.id()).thenReturn("internal-id");
        when(client.clientId()).thenReturn("ecofy-web");
        when(client.name()).thenReturn("EcoFy Web");
        when(client.clientType()).thenReturn(nonConfidentialClientType());
        when(client.grantTypes()).thenReturn(grantTypes);
        when(client.redirectUris()).thenReturn(redirectUris);
        when(client.scopes()).thenReturn(scopes);
        when(client.isFirstParty()).thenReturn(true);
        when(client.isActive()).thenReturn(true);
        when(client.createdAt()).thenReturn(CREATED_AT);
        when(client.updatedAt()).thenReturn(UPDATED_AT);

        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        assertEquals(Set.of(grantType), response.grantTypes());
        assertEquals(Set.of("https://app.ecofy.com/callback"), response.redirectUris());
        assertEquals(Set.of("openid", "profile"), response.scopes());

        assertEquals(1, response.grantTypes().size());
        assertEquals(1, response.redirectUris().size());
        assertEquals(2, response.scopes().size());

        assertTrue(response.grantTypes().stream().noneMatch(Objects::isNull));
        assertTrue(response.redirectUris().stream().noneMatch(Objects::isNull));
        assertTrue(response.scopes().stream().noneMatch(Objects::isNull));
    }

    @Test
    void shouldReturnUnmodifiableSetsInResponse() {
        GrantType grantType = anyGrantType();

        ClientApplication client = new ClientApplication(
                "internal-id",
                "ecofy-web",
                null,
                "EcoFy Web",
                nonConfidentialClientType(),
                Set.of(grantType),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        assertThrows(
                UnsupportedOperationException.class,
                () -> response.grantTypes().add(grantType)
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> response.redirectUris().add("https://evil.com/callback")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> response.scopes().add("admin")
        );
    }

    @Test
    void shouldThrowExceptionWhenClientIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ClientApplicationMapper.toResponse(null)
        );

        assertEquals("client must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnEmptyListWhenClientListIsNull() {
        List<ClientApplicationResponse> responses = ClientApplicationMapper.toResponseList(null);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenClientListIsEmpty() {
        List<ClientApplicationResponse> responses = ClientApplicationMapper.toResponseList(List.of());

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldMapClientListFilteringNullValues() {
        GrantType grantType = anyGrantType();

        ClientApplication firstClient = new ClientApplication(
                "internal-id-1",
                "ecofy-web",
                null,
                "EcoFy Web",
                nonConfidentialClientType(),
                Set.of(grantType),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        ClientApplication secondClient = new ClientApplication(
                "internal-id-2",
                "ecofy-mobile",
                null,
                "EcoFy Mobile",
                nonConfidentialClientType(),
                Set.of(),
                Set.of(),
                Set.of(),
                false,
                false,
                CREATED_AT,
                UPDATED_AT
        );

        List<ClientApplicationResponse> responses = ClientApplicationMapper.toResponseList(
                Arrays.asList(firstClient, null, secondClient)
        );

        assertEquals(2, responses.size());

        assertEquals("internal-id-1", responses.get(0).id());
        assertEquals("ecofy-web", responses.get(0).clientId());
        assertEquals("EcoFy Web", responses.get(0).name());
        assertEquals(Set.of(grantType), responses.get(0).grantTypes());
        assertEquals(Set.of("https://app.ecofy.com/callback"), responses.get(0).redirectUris());
        assertEquals(Set.of("openid"), responses.get(0).scopes());
        assertTrue(responses.get(0).firstParty());
        assertTrue(responses.get(0).active());

        assertEquals("internal-id-2", responses.get(1).id());
        assertEquals("ecofy-mobile", responses.get(1).clientId());
        assertEquals("EcoFy Mobile", responses.get(1).name());
        assertTrue(responses.get(1).grantTypes().isEmpty());
        assertTrue(responses.get(1).redirectUris().isEmpty());
        assertTrue(responses.get(1).scopes().isEmpty());
        assertFalse(responses.get(1).firstParty());
        assertFalse(responses.get(1).active());
    }

    @Test
    void shouldReturnUnmodifiableResponseList() {
        ClientApplication client = new ClientApplication(
                "internal-id",
                "ecofy-web",
                null,
                "EcoFy Web",
                nonConfidentialClientType(),
                Set.of(anyGrantType()),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid"),
                true,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        List<ClientApplicationResponse> responses = ClientApplicationMapper.toResponseList(List.of(client));

        assertEquals(1, responses.size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> responses.add(ClientApplicationMapper.toResponse(client))
        );
    }

    @Test
    void shouldInstantiatePrivateConstructorForCoverage() throws Exception {
        Constructor<ClientApplicationMapper> constructor =
                ClientApplicationMapper.class.getDeclaredConstructor();

        constructor.setAccessible(true);

        ClientApplicationMapper instance = constructor.newInstance();

        assertNotNull(instance);
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