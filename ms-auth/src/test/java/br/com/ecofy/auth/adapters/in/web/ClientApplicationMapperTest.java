package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.response.ClientApplicationResponse;
import br.com.ecofy.auth.adapters.in.web.mapper.ClientApplicationMapper;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientApplicationMapperTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ClientApplication clientApplication;

    @Test
    void constructor_shouldNotBePublic_andCallableViaReflection() throws Exception {

        Constructor<ClientApplicationMapper> ctor =
                ClientApplicationMapper.class.getDeclaredConstructor();

        assertFalse(Modifier.isPublic(ctor.getModifiers()));

        ctor.setAccessible(true);
        ClientApplicationMapper instance = ctor.newInstance();
        assertNotNull(instance);

    }

    @Test
    void toResponse_shouldMapAllFields_andFilterNullsInSets_whenIdIsNotNull() {

        // Arrange
        UUID id = UUID.randomUUID();

        when(clientApplication.id()).thenReturn(String.valueOf(id));
        when(clientApplication.clientId()).thenReturn("client-123");
        when(clientApplication.name()).thenReturn("My Client");
        when(clientApplication.isFirstParty()).thenReturn(true);
        when(clientApplication.isActive()).thenReturn(true);

        when(clientApplication.clientType()).thenReturn(null);

        // Sets com valores e nulls para exercitar safeGrants/safeStrings
        Set<GrantType> grantTypes = new HashSet<>(Arrays.asList(
                GrantType.AUTHORIZATION_CODE,
                null,
                GrantType.CLIENT_CREDENTIALS
        ));
        Set<String> redirectUris = new HashSet<>(Arrays.asList(
                "https://example.com/callback",
                null
        ));
        Set<String> scopes = new HashSet<>(Arrays.asList(
                "openid",
                "profile",
                null
        ));

        when(clientApplication.grantTypes()).thenReturn(grantTypes);
        when(clientApplication.redirectUris()).thenReturn(redirectUris);
        when(clientApplication.scopes()).thenReturn(scopes);

        // Act
        ClientApplicationResponse response = ClientApplicationMapper.toResponse(clientApplication);

        // Assert
        assertNotNull(response);
        assertEquals(id.toString(), response.id());
        assertEquals("client-123", response.clientId());
        assertEquals("My Client", response.name());

        assertNull(response.clientType());
        assertTrue(response.firstParty());
        assertTrue(response.active());

        assertEquals(Set.of(GrantType.AUTHORIZATION_CODE, GrantType.CLIENT_CREDENTIALS),
                response.grantTypes());
        assertEquals(Set.of("https://example.com/callback"), response.redirectUris());
        assertEquals(Set.of("openid", "profile"), response.scopes());

        assertThrows(UnsupportedOperationException.class,
                () -> response.grantTypes().add(GrantType.REFRESH_TOKEN));

    }


    @Test
    void toResponse_shouldGenerateRandomIdAndReturnEmptySets_whenIdAndSetsAreNullOrEmpty() {

        // Arrange
        ClientApplication client = mock(ClientApplication.class);

        when(client.id()).thenReturn(null);
        when(client.clientId()).thenReturn("client-xyz");
        when(client.name()).thenReturn("Client Null");
        when(client.isFirstParty()).thenReturn(false);
        when(client.isActive()).thenReturn(false);

        // grantTypes / redirectUris / scopes null para cair nos branches de safe*
        when(client.grantTypes()).thenReturn(null);
        when(client.redirectUris()).thenReturn(null);
        when(client.scopes()).thenReturn(Collections.emptySet());

        // Act
        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());
        assertFalse(response.id().isBlank(), "id gerado não pode ser em branco");

        assertEquals("client-xyz", response.clientId());
        assertEquals("Client Null", response.name());
        assertFalse(response.firstParty());
        assertFalse(response.active());

        assertTrue(response.grantTypes().isEmpty());
        assertTrue(response.redirectUris().isEmpty());
        assertTrue(response.scopes().isEmpty());

    }

    @Test
    void toResponse_shouldThrowNullPointerException_whenClientIsNull() {

        assertThrows(NullPointerException.class,
                () -> ClientApplicationMapper.toResponse(null));

    }


    @Test
    void toResponseList_shouldReturnEmptyList_whenInputIsNullOrEmpty() {

        // null
        List<ClientApplicationResponse> fromNull = ClientApplicationMapper.toResponseList(null);
        assertNotNull(fromNull);
        assertTrue(fromNull.isEmpty());

        // vazio
        List<ClientApplicationResponse> fromEmpty = ClientApplicationMapper.toResponseList(List.of());
        assertNotNull(fromEmpty);
        assertTrue(fromEmpty.isEmpty());

    }

    @Test
    void toResponseList_shouldMapClients_filterNullsAndReturnUnmodifiableList() {

        // Arrange
        ClientApplication client1 = mock(ClientApplication.class);
        ClientApplication client2 = mock(ClientApplication.class);

        when(client1.id()).thenReturn(String.valueOf(UUID.fromString("00000000-0000-0000-0000-000000000001")));
        when(client1.clientId()).thenReturn("client-1");
        when(client1.name()).thenReturn("Client 1");
        when(client1.isFirstParty()).thenReturn(true);
        when(client1.isActive()).thenReturn(true);
        when(client1.grantTypes()).thenReturn(Set.of(GrantType.AUTHORIZATION_CODE));
        when(client1.redirectUris()).thenReturn(Set.of("https://one/cb"));
        when(client1.scopes()).thenReturn(Set.of("openid"));

        when(client2.id()).thenReturn(String.valueOf(UUID.fromString("00000000-0000-0000-0000-000000000002")));
        when(client2.clientId()).thenReturn("client-2");
        when(client2.name()).thenReturn("Client 2");
        when(client2.isFirstParty()).thenReturn(false);
        when(client2.isActive()).thenReturn(false);
        when(client2.grantTypes()).thenReturn(Set.of(GrantType.CLIENT_CREDENTIALS));
        when(client2.redirectUris()).thenReturn(Set.of());
        when(client2.scopes()).thenReturn(Set.of("api.read"));

        List<ClientApplication> input = Arrays.asList(
                client1,
                null,
                client2
        );

        // Act
        List<ClientApplicationResponse> responses = ClientApplicationMapper.toResponseList(input);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());

        ClientApplicationResponse r1 = responses.get(0);
        ClientApplicationResponse r2 = responses.get(1);

        assertEquals("00000000-0000-0000-0000-000000000001", r1.id());
        assertEquals("client-1", r1.clientId());
        assertTrue(r1.firstParty());
        assertTrue(r1.active());
        assertEquals(Set.of(GrantType.AUTHORIZATION_CODE), r1.grantTypes());
        assertEquals(Set.of("https://one/cb"), r1.redirectUris());
        assertEquals(Set.of("openid"), r1.scopes());

        assertEquals("00000000-0000-0000-0000-000000000002", r2.id());
        assertEquals("client-2", r2.clientId());
        assertFalse(r2.firstParty());
        assertFalse(r2.active());
        assertEquals(Set.of(GrantType.CLIENT_CREDENTIALS), r2.grantTypes());
        assertTrue(r2.redirectUris().isEmpty());
        assertEquals(Set.of("api.read"), r2.scopes());

        // lista deve ser imutável
        assertThrows(UnsupportedOperationException.class,
                () -> responses.add(r1));

    }

}
