package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.ClientApplicationEntity;
import br.com.ecofy.auth.adapters.out.persistence.repository.ClientApplicationRepository;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do adaptador JPA de aplicações cliente")
class ClientApplicationJpaAdapterTest {

    private static final String ID = "application-id";
    private static final String CLIENT_ID = "ecofy-client";
    private static final String CLIENT_SECRET_HASH = "client-secret-hash";
    private static final String NAME = "Aplicação EcoFy";
    private static final Instant CREATED_AT =
            Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant UPDATED_AT =
            Instant.parse("2026-07-20T11:00:00Z");

    private static final ClientType CLIENT_TYPE =
            ClientType.values()[0];
    private static final GrantType GRANT_TYPE =
            GrantType.values()[0];

    @Mock
    private ClientApplicationRepository repository;

    @Test
    @DisplayName("Deve rejeitar repositório nulo ao construir o adaptador")
    void constructor_repositorioNulo_deveLancarNullPointerException() {
        // Arrange
        ClientApplicationRepository nullRepository = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ClientApplicationJpaAdapter(nullRepository)
        );

        // Assert
        assertEquals(
                "repository must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar aplicação cliente nula sem acessar o repositório")
    void save_clientApplicationNula_deveLancarNullPointerException() {
        // Arrange
        ClientApplicationJpaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.save(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "clientApplication must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(repository)
        );
    }

    @Test
    @DisplayName("Deve preservar a data de criação e atualizar a data de modificação ao salvar")
    void save_aplicacaoValida_devePreservarCreatedAtEAtualizarUpdatedAt() {
        // Arrange
        ClientApplication clientApplication = createClientApplication(
                CREATED_AT,
                UPDATED_AT
        );

        when(repository.save(any(ClientApplicationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientApplicationJpaAdapter adapter = createAdapter();
        ArgumentCaptor<ClientApplicationEntity> captor =
                ArgumentCaptor.forClass(ClientApplicationEntity.class);

        Instant beforeSave = Instant.now();

        // Act
        ClientApplication result = adapter.save(clientApplication);

        Instant afterSave = Instant.now();

        // Assert
        verify(repository).save(captor.capture());

        ClientApplicationEntity savedEntity = captor.getValue();

        assertAll(
                () -> assertEquals(ID, savedEntity.getId()),
                () -> assertEquals(CLIENT_ID, savedEntity.getClientId()),
                () -> assertEquals(
                        CLIENT_SECRET_HASH,
                        savedEntity.getClientSecretHash()
                ),
                () -> assertEquals(NAME, savedEntity.getName()),
                () -> assertEquals(
                        CLIENT_TYPE,
                        savedEntity.getClientType()
                ),
                () -> assertEquals(
                        Set.of(GRANT_TYPE),
                        savedEntity.getGrantTypes()
                ),
                () -> assertEquals(
                        Set.of("https://ecofy.com/callback"),
                        savedEntity.getRedirectUris()
                ),
                () -> assertEquals(
                        Set.of("openid", "profile"),
                        savedEntity.getScopes()
                ),
                () -> assertTrue(savedEntity.isFirstParty()),
                () -> assertTrue(savedEntity.isActive()),
                () -> assertEquals(
                        CREATED_AT,
                        savedEntity.getCreatedAt()
                ),
                () -> assertFalse(
                        savedEntity.getUpdatedAt().isBefore(beforeSave)
                ),
                () -> assertFalse(
                        savedEntity.getUpdatedAt().isAfter(afterSave)
                ),
                () -> assertEquals(
                        CREATED_AT,
                        result.createdAt()
                ),
                () -> assertEquals(
                        savedEntity.getUpdatedAt(),
                        result.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve preencher createdAt quando a aplicação mapeada não possuir data de criação")
    void save_aplicacaoSemCreatedAt_devePreencherCreatedAtComDataAtual() {
        // Arrange
        ClientApplication clientApplication = mock(ClientApplication.class);

        when(clientApplication.id()).thenReturn(ID);
        when(clientApplication.clientId()).thenReturn(CLIENT_ID);
        when(clientApplication.clientSecretHash())
                .thenReturn(CLIENT_SECRET_HASH);
        when(clientApplication.name()).thenReturn(NAME);
        when(clientApplication.clientType()).thenReturn(CLIENT_TYPE);
        when(clientApplication.grantTypes())
                .thenReturn(Set.of(GRANT_TYPE));
        when(clientApplication.redirectUris())
                .thenReturn(Set.of("https://ecofy.com/callback"));
        when(clientApplication.scopes())
                .thenReturn(Set.of("openid", "profile"));
        when(clientApplication.isFirstParty()).thenReturn(true);
        when(clientApplication.isActive()).thenReturn(true);
        when(clientApplication.createdAt()).thenReturn(null);
        when(clientApplication.updatedAt()).thenReturn(null);

        when(repository.save(any(ClientApplicationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientApplicationJpaAdapter adapter = createAdapter();
        ArgumentCaptor<ClientApplicationEntity> captor =
                ArgumentCaptor.forClass(ClientApplicationEntity.class);

        Instant beforeSave = Instant.now();

        // Act
        ClientApplication result = adapter.save(clientApplication);

        Instant afterSave = Instant.now();

        // Assert
        verify(repository).save(captor.capture());

        ClientApplicationEntity savedEntity = captor.getValue();

        assertAll(
                () -> assertNotNull(savedEntity.getCreatedAt()),
                () -> assertNotNull(savedEntity.getUpdatedAt()),
                () -> assertEquals(
                        savedEntity.getCreatedAt(),
                        savedEntity.getUpdatedAt()
                ),
                () -> assertFalse(
                        savedEntity.getCreatedAt().isBefore(beforeSave)
                ),
                () -> assertFalse(
                        savedEntity.getCreatedAt().isAfter(afterSave)
                ),
                () -> assertEquals(
                        savedEntity.getCreatedAt(),
                        result.createdAt()
                ),
                () -> assertEquals(
                        savedEntity.getUpdatedAt(),
                        result.updatedAt()
                ),
                () -> assertEquals(ID, result.id()),
                () -> assertEquals(CLIENT_ID, result.clientId())
        );
    }

    @Test
    @DisplayName("Deve preservar createdAt quando a aplicação já possuir data de criação")
    void save_aplicacaoComCreatedAt_devePreservarCreatedAtEAtualizarUpdatedAt() {
        // Arrange
        ClientApplication clientApplication = createClientApplication(
                CREATED_AT,
                UPDATED_AT
        );

        when(repository.save(any(ClientApplicationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientApplicationJpaAdapter adapter = createAdapter();
        ArgumentCaptor<ClientApplicationEntity> captor =
                ArgumentCaptor.forClass(ClientApplicationEntity.class);

        Instant beforeSave = Instant.now();

        // Act
        ClientApplication result = adapter.save(clientApplication);

        Instant afterSave = Instant.now();

        // Assert
        verify(repository).save(captor.capture());

        ClientApplicationEntity savedEntity = captor.getValue();

        assertAll(
                () -> assertEquals(
                        CREATED_AT,
                        savedEntity.getCreatedAt()
                ),
                () -> assertNotNull(savedEntity.getUpdatedAt()),
                () -> assertFalse(
                        savedEntity.getUpdatedAt().isBefore(beforeSave)
                ),
                () -> assertFalse(
                        savedEntity.getUpdatedAt().isAfter(afterSave)
                ),
                () -> assertEquals(
                        CREATED_AT,
                        result.createdAt()
                ),
                () -> assertEquals(
                        savedEntity.getUpdatedAt(),
                        result.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve preservar a data de criação e atualizar updatedAt ao salvar aplicação existente")
    void save_aplicacaoComCreatedAt_devePreservarCriacaoEAtualizarUpdatedAt()
            throws Exception {
        // Arrange
        ClientApplication clientApplication = createClientApplication(
                CREATED_AT,
                UPDATED_AT
        );

        when(repository.save(any(ClientApplicationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClientApplicationJpaAdapter adapter = createAdapter();
        ArgumentCaptor<ClientApplicationEntity> captor =
                ArgumentCaptor.forClass(ClientApplicationEntity.class);

        Instant beforeSave = Instant.now();

        // Act
        ClientApplication result = adapter.save(clientApplication);

        Instant afterSave = Instant.now();

        // Assert
        verify(repository).save(captor.capture());
        ClientApplicationEntity savedEntity = captor.getValue();

        assertAll(
                () -> assertEquals(
                        CREATED_AT,
                        savedEntity.getCreatedAt()
                ),
                () -> assertNotNull(savedEntity.getUpdatedAt()),
                () -> assertFalse(
                        savedEntity.getUpdatedAt().isBefore(beforeSave)
                ),
                () -> assertFalse(
                        savedEntity.getUpdatedAt().isAfter(afterSave)
                ),
                () -> assertEquals(
                        CREATED_AT,
                        result.createdAt()
                ),
                () -> assertEquals(
                        savedEntity.getUpdatedAt(),
                        result.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar clientId nulo sem consultar o repositório")
    void loadByClientId_clientIdNulo_deveLancarNullPointerException() {
        // Arrange
        ClientApplicationJpaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.loadByClientId(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "clientId must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(repository)
        );
    }

    @Test
    @DisplayName("Deve consultar o repositório mesmo quando o clientId estiver em branco")
    void loadByClientId_clientIdEmBranco_deveConsultarERetornarVazio() {
        // Arrange
        String blankClientId = "   ";

        when(repository.findByClientId(blankClientId))
                .thenReturn(Optional.empty());

        ClientApplicationJpaAdapter adapter = createAdapter();

        // Act
        Optional<ClientApplication> result =
                adapter.loadByClientId(blankClientId);

        // Assert
        assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> verify(repository)
                        .findByClientId(blankClientId)
        );
    }

    @Test
    @DisplayName("Deve retornar vazio quando não existir aplicação com o clientId informado")
    void loadByClientId_aplicacaoInexistente_deveRetornarOptionalVazio() {
        // Arrange
        when(repository.findByClientId(CLIENT_ID))
                .thenReturn(Optional.empty());

        ClientApplicationJpaAdapter adapter = createAdapter();

        // Act
        Optional<ClientApplication> result =
                adapter.loadByClientId(CLIENT_ID);

        // Assert
        assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> verify(repository).findByClientId(CLIENT_ID)
        );
    }

    @Test
    @DisplayName("Deve converter e retornar a aplicação encontrada pelo clientId")
    void loadByClientId_aplicacaoExistente_deveRetornarAplicacaoMapeada() {
        // Arrange
        ClientApplicationEntity entity = createClientApplicationEntity();

        when(repository.findByClientId(CLIENT_ID))
                .thenReturn(Optional.of(entity));

        ClientApplicationJpaAdapter adapter = createAdapter();

        // Act
        Optional<ClientApplication> result =
                adapter.loadByClientId(CLIENT_ID);

        // Assert
        ClientApplication clientApplication = result.orElseThrow();

        assertAll(
                () -> assertEquals(ID, clientApplication.id()),
                () -> assertEquals(
                        CLIENT_ID,
                        clientApplication.clientId()
                ),
                () -> assertEquals(
                        CLIENT_SECRET_HASH,
                        clientApplication.clientSecretHash()
                ),
                () -> assertEquals(
                        NAME,
                        clientApplication.name()
                ),
                () -> assertEquals(
                        CLIENT_TYPE,
                        clientApplication.clientType()
                ),
                () -> assertEquals(
                        Set.of(GRANT_TYPE),
                        clientApplication.grantTypes()
                ),
                () -> assertEquals(
                        Set.of("https://ecofy.com/callback"),
                        clientApplication.redirectUris()
                ),
                () -> assertEquals(
                        Set.of("openid", "profile"),
                        clientApplication.scopes()
                ),
                () -> assertTrue(clientApplication.isFirstParty()),
                () -> assertTrue(clientApplication.isActive()),
                () -> assertEquals(
                        CREATED_AT,
                        clientApplication.createdAt()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        clientApplication.updatedAt()
                )
        );

        verify(repository).findByClientId(CLIENT_ID);
    }

    private ClientApplicationJpaAdapter createAdapter() {
        return new ClientApplicationJpaAdapter(repository);
    }

    private ClientApplication createClientApplication(
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ClientApplication(
                ID,
                CLIENT_ID,
                CLIENT_SECRET_HASH,
                NAME,
                CLIENT_TYPE,
                Set.of(GRANT_TYPE),
                Set.of("https://ecofy.com/callback"),
                Set.of("openid", "profile"),
                true,
                true,
                createdAt,
                updatedAt
        );
    }

    private ClientApplicationEntity createClientApplicationEntity() {
        return ClientApplicationEntity.builder()
                .id(ID)
                .clientId(CLIENT_ID)
                .clientSecretHash(CLIENT_SECRET_HASH)
                .name(NAME)
                .clientType(CLIENT_TYPE)
                .grantTypes(Set.of(GRANT_TYPE))
                .redirectUris(Set.of("https://ecofy.com/callback"))
                .scopes(Set.of("openid", "profile"))
                .firstParty(true)
                .active(true)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }
}
