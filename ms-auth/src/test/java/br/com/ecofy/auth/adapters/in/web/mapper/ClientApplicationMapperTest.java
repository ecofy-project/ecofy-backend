package br.com.ecofy.auth.adapters.in.web.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.ecofy.auth.adapters.in.web.dto.response.ClientApplicationResponse;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes unitários do mapeador de aplicações cliente")
class ClientApplicationMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T10:00:00Z");

    @Test
    @DisplayName("Deve converter a aplicação cliente preenchida para a resposta correspondente")
    void toResponse_clientePreenchido_deveConverterTodosOsCampos() {
        // Arrange
        GrantType grantType = anyGrantType();
        ClientType clientType = nonConfidentialClientType();

        ClientApplication client = new ClientApplication(
                "internal-id",
                "ecofy-web",
                null,
                "EcoFy Web",
                clientType,
                Set.of(grantType),
                Set.of("https://app.ecofy.com/callback"),
                Set.of("openid", "profile"),
                true,
                true,
                CREATED_AT,
                UPDATED_AT
        );

        // Act
        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        // Assert
        assertEquals("internal-id", response.id());
        assertEquals("ecofy-web", response.clientId());
        assertEquals("EcoFy Web", response.name());
        assertEquals(clientType, response.clientType());
        assertEquals(Set.of(grantType), response.grantTypes());
        assertEquals(Set.of("https://app.ecofy.com/callback"), response.redirectUris());
        assertEquals(Set.of("openid", "profile"), response.scopes());
        assertTrue(response.firstParty());
        assertTrue(response.active());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(UPDATED_AT, response.updatedAt());
    }

    @Test
    @DisplayName("Deve gerar um identificador UUID quando a aplicação não possuir identificador interno")
    void toResponse_identificadorNulo_deveGerarIdentificadorUuid() {
        // Arrange
        GrantType grantType = anyGrantType();
        ClientType clientType = nonConfidentialClientType();
        ClientApplication client = mock(ClientApplication.class);

        when(client.id()).thenReturn(null);
        when(client.clientId()).thenReturn("ecofy-web");
        when(client.name()).thenReturn("EcoFy Web");
        when(client.clientType()).thenReturn(clientType);
        when(client.grantTypes()).thenReturn(Set.of(grantType));
        when(client.redirectUris()).thenReturn(Set.of("https://app.ecofy.com/callback"));
        when(client.scopes()).thenReturn(Set.of("openid"));
        when(client.isFirstParty()).thenReturn(false);
        when(client.isActive()).thenReturn(true);
        when(client.createdAt()).thenReturn(CREATED_AT);
        when(client.updatedAt()).thenReturn(UPDATED_AT);

        // Act
        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        // Assert
        assertNotNull(response.id());
        assertFalse(response.id().isBlank());
        assertDoesNotThrow(() -> UUID.fromString(response.id()));
        assertEquals("ecofy-web", response.clientId());
        assertEquals("EcoFy Web", response.name());
        assertEquals(clientType, response.clientType());
        assertEquals(Set.of(grantType), response.grantTypes());
        assertEquals(Set.of("https://app.ecofy.com/callback"), response.redirectUris());
        assertEquals(Set.of("openid"), response.scopes());
        assertFalse(response.firstParty());
        assertTrue(response.active());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(UPDATED_AT, response.updatedAt());
    }

    @Test
    @DisplayName("Deve retornar conjuntos vazios quando as coleções da aplicação forem nulas")
    void toResponse_colecoesNulas_deveRetornarConjuntosVazios() {
        // Arrange
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

        // Act
        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        // Assert
        assertNotNull(response.grantTypes());
        assertNotNull(response.redirectUris());
        assertNotNull(response.scopes());
        assertTrue(response.grantTypes().isEmpty());
        assertTrue(response.redirectUris().isEmpty());
        assertTrue(response.scopes().isEmpty());
        assertFalse(response.firstParty());
        assertFalse(response.active());
    }

    @Test
    @DisplayName("Deve preservar conjuntos vazios quando a aplicação não possuir valores nas coleções")
    void toResponse_colecoesVazias_deveRetornarConjuntosVazios() {
        // Arrange
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

        // Act
        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        // Assert
        assertTrue(response.grantTypes().isEmpty());
        assertTrue(response.redirectUris().isEmpty());
        assertTrue(response.scopes().isEmpty());
        assertTrue(response.firstParty());
        assertTrue(response.active());
    }

    @Test
    @DisplayName("Deve remover valores nulos das coleções ao converter a aplicação")
    void toResponse_colecoesComValoresNulos_deveFiltrarValoresNulos() {
        // Arrange
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

        // Act
        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        // Assert
        assertEquals(Set.of(grantType), response.grantTypes());
        assertEquals(Set.of("https://app.ecofy.com/callback"), response.redirectUris());
        assertEquals(Set.of("openid", "profile"), response.scopes());
        assertTrue(response.grantTypes().stream().noneMatch(Objects::isNull));
        assertTrue(response.redirectUris().stream().noneMatch(Objects::isNull));
        assertTrue(response.scopes().stream().noneMatch(Objects::isNull));
    }

    @Test
    @DisplayName("Deve retornar conjuntos imutáveis ao converter a aplicação cliente")
    void toResponse_clienteValido_deveRetornarConjuntosImutaveis() {
        // Arrange
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

        // Act
        ClientApplicationResponse response = ClientApplicationMapper.toResponse(client);

        // Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> response.grantTypes().add(grantType)
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> response.redirectUris().add("https://outro.exemplo/callback")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> response.scopes().add("admin")
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando a aplicação cliente for nula")
    void toResponse_clienteNulo_deveLancarNullPointerException() {
        // Arrange
        ClientApplication client = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ClientApplicationMapper.toResponse(client)
        );

        // Assert
        assertEquals("client must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando a lista de aplicações for nula")
    void toResponseList_listaNula_deveRetornarListaVazia() {
        // Arrange
        List<ClientApplication> clients = null;

        // Act
        List<ClientApplicationResponse> responses =
                ClientApplicationMapper.toResponseList(clients);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver aplicações cliente")
    void toResponseList_listaVazia_deveRetornarListaVazia() {
        // Arrange
        List<ClientApplication> clients = List.of();

        // Act
        List<ClientApplicationResponse> responses =
                ClientApplicationMapper.toResponseList(clients);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("Deve converter a lista de aplicações ignorando elementos nulos")
    void toResponseList_listaComElementoNulo_deveConverterElementosValidos() {
        // Arrange
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

        List<ClientApplication> clients = Arrays.asList(
                firstClient,
                null,
                secondClient
        );

        // Act
        List<ClientApplicationResponse> responses =
                ClientApplicationMapper.toResponseList(clients);

        // Assert
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
    @DisplayName("Deve retornar lista imutável após converter as aplicações cliente")
    void toResponseList_listaValida_deveRetornarListaImutavel() {
        // Arrange
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

        // Act
        List<ClientApplicationResponse> responses =
                ClientApplicationMapper.toResponseList(List.of(client));

        // Assert
        assertEquals(1, responses.size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> responses.add(ClientApplicationMapper.toResponse(client))
        );
    }

    @Test
    @DisplayName("Deve manter o construtor privado da classe utilitária")
    void constructor_classeUtilitaria_deveSerPrivadoEExecutavelPorReflexao() throws Exception {
        // Arrange
        Constructor<ClientApplicationMapper> constructor =
                ClientApplicationMapper.class.getDeclaredConstructor();

        // Act
        constructor.setAccessible(true);
        ClientApplicationMapper instance = constructor.newInstance();

        // Assert
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        assertNotNull(instance);
    }

    private static GrantType anyGrantType() {
        return Arrays.stream(GrantType.values())
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("GrantType enum must have at least one value")
                );
    }

    private static ClientType nonConfidentialClientType() {
        return Arrays.stream(ClientType.values())
                .filter(clientType -> clientType != ClientType.CONFIDENTIAL)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "ClientType enum must have at least one non-CONFIDENTIAL value"
                        )
                );
    }
}
