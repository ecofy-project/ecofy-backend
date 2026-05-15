package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.ClientApplicationEntity;
import br.com.ecofy.auth.adapters.out.persistence.mapper.PersistenceMapper;
import br.com.ecofy.auth.adapters.out.persistence.repository.ClientApplicationRepository;
import br.com.ecofy.auth.core.domain.ClientApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientApplicationJpaAdapterTest {

    @Mock
    private ClientApplicationRepository repository;

    @Test
    void constructor_shouldRejectNullRepository() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new ClientApplicationJpaAdapter(null));
        assertEquals("repository must not be null", ex.getMessage());
    }

    @Test
    void save_shouldRejectNullClientApplication() {
        ClientApplicationJpaAdapter adapter = new ClientApplicationJpaAdapter(repository);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.save(null));
        assertEquals("clientApplication must not be null", ex.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void save_shouldSetCreatedAtAndUpdatedAt_whenEntityHasNullCreatedAt() {
        ClientApplicationJpaAdapter adapter = new ClientApplicationJpaAdapter(repository);

        ClientApplication domain = mock(ClientApplication.class);
        when(domain.clientId()).thenReturn("client-1");
        when(domain.name()).thenReturn("App 1");

        ClientApplicationEntity entity = new ClientApplicationEntity();
        entity.setClientId("client-1");
        entity.setCreatedAt(null);

        ArgumentCaptor<ClientApplicationEntity> captor = ArgumentCaptor.forClass(ClientApplicationEntity.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        ClientApplication mappedDomain = mock(ClientApplication.class);
        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toEntity(same(domain))).thenReturn(entity);
            mocked.when(() -> PersistenceMapper.toDomain(any(ClientApplicationEntity.class))).thenReturn(mappedDomain);

            ClientApplication result = adapter.save(domain);
            assertSame(mappedDomain, result);
        }

        ClientApplicationEntity persisted = captor.getValue();
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
        assertFalse(persisted.getUpdatedAt().isBefore(persisted.getCreatedAt()));

        verify(repository).save(any(ClientApplicationEntity.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_shouldKeepCreatedAt_whenEntityAlreadyHasCreatedAt_andAlwaysUpdateUpdatedAt() {
        ClientApplicationJpaAdapter adapter = new ClientApplicationJpaAdapter(repository);

        ClientApplication domain = mock(ClientApplication.class);
        when(domain.clientId()).thenReturn("client-2");
        when(domain.name()).thenReturn("App 2");

        Instant createdAt = Instant.parse("2020-01-01T00:00:00Z");
        ClientApplicationEntity entity = new ClientApplicationEntity();
        entity.setClientId("client-2");
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(null);

        ArgumentCaptor<ClientApplicationEntity> captor = ArgumentCaptor.forClass(ClientApplicationEntity.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        ClientApplication mappedDomain = mock(ClientApplication.class);
        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toEntity(same(domain))).thenReturn(entity);
            mocked.when(() -> PersistenceMapper.toDomain(any(ClientApplicationEntity.class))).thenReturn(mappedDomain);

            ClientApplication result = adapter.save(domain);
            assertSame(mappedDomain, result);
        }

        ClientApplicationEntity persisted = captor.getValue();
        assertEquals(createdAt, persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
        assertFalse(persisted.getUpdatedAt().isBefore(persisted.getCreatedAt()));

        verify(repository).save(any(ClientApplicationEntity.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void loadByClientId_shouldRejectNullClientId() {
        ClientApplicationJpaAdapter adapter = new ClientApplicationJpaAdapter(repository);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.loadByClientId(null));
        assertEquals("clientId must not be null", ex.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void loadByClientId_shouldReturnEmpty_whenNotFound() {
        ClientApplicationJpaAdapter adapter = new ClientApplicationJpaAdapter(repository);

        when(repository.findByClientId("missing")).thenReturn(Optional.empty());

        Optional<ClientApplication> result = adapter.loadByClientId("missing");

        assertTrue(result.isEmpty());
        verify(repository).findByClientId("missing");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void loadByClientId_shouldMapToDomain_whenFound() {
        ClientApplicationJpaAdapter adapter = new ClientApplicationJpaAdapter(repository);

        ClientApplicationEntity entity = new ClientApplicationEntity();
        entity.setClientId("client-3");

        when(repository.findByClientId("client-3")).thenReturn(Optional.of(entity));

        ClientApplication mappedDomain = mock(ClientApplication.class);
        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toDomain(same(entity))).thenReturn(mappedDomain);

            Optional<ClientApplication> result = adapter.loadByClientId("client-3");

            assertTrue(result.isPresent());
            assertSame(mappedDomain, result.get());
        }

        verify(repository).findByClientId("client-3");
        verifyNoMoreInteractions(repository);
    }
}