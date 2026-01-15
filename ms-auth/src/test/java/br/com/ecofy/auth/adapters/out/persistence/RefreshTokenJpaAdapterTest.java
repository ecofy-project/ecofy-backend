package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.RefreshTokenEntity;
import br.com.ecofy.auth.adapters.out.persistence.mapper.PersistenceMapper;
import br.com.ecofy.auth.adapters.out.persistence.repository.RefreshTokenRepository;
import br.com.ecofy.auth.core.domain.RefreshToken;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenJpaAdapterTest {

    @org.mockito.Mock
    private RefreshTokenRepository repository;

    @Test
    void constructor_shouldRejectNullRepository() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new RefreshTokenJpaAdapter(null));
        assertEquals("repository must not be null", ex.getMessage());
    }

    @Test
    void save_shouldRejectNullToken() {
        RefreshTokenJpaAdapter adapter = new RefreshTokenJpaAdapter(repository);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.save(null));
        assertEquals("token must not be null", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void save_shouldMapToEntity_saveAndMapBackToDomain() {
        RefreshTokenJpaAdapter adapter = new RefreshTokenJpaAdapter(repository);

        RefreshToken token = mock(RefreshToken.class);
        when(token.id()).thenReturn(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        AuthUserId userId = mock(AuthUserId.class);
        when(userId.value()).thenReturn(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        when(token.userId()).thenReturn(userId);
        when(token.clientId()).thenReturn("client-1");

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken mappedDomain = mock(RefreshToken.class);

        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toEntity(same(token))).thenReturn(entity);
            mocked.when(() -> PersistenceMapper.toDomain(any(RefreshTokenEntity.class))).thenReturn(mappedDomain);

            RefreshToken result = adapter.save(token);

            assertSame(mappedDomain, result);
        }

        assertSame(entity, captor.getValue());
        verify(repository).save(any(RefreshTokenEntity.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findByTokenValue_shouldRejectNullTokenValue() {
        RefreshTokenJpaAdapter adapter = new RefreshTokenJpaAdapter(repository);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.findByTokenValue(null));
        assertEquals("tokenValue must not be null", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void findByTokenValue_shouldReturnEmpty_whenNotFound() {
        RefreshTokenJpaAdapter adapter = new RefreshTokenJpaAdapter(repository);

        when(repository.findByTokenValue("missing")).thenReturn(Optional.empty());

        Optional<RefreshToken> result = adapter.findByTokenValue("missing");

        assertTrue(result.isEmpty());
        verify(repository).findByTokenValue("missing");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findByTokenValue_shouldMapToDomain_whenFound() {
        RefreshTokenJpaAdapter adapter = new RefreshTokenJpaAdapter(repository);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
        entity.setRevoked(false);

        when(repository.findByTokenValue("tv")).thenReturn(Optional.of(entity));

        RefreshToken mappedDomain = mock(RefreshToken.class);

        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toDomain(same(entity))).thenReturn(mappedDomain);

            Optional<RefreshToken> result = adapter.findByTokenValue("tv");

            assertTrue(result.isPresent());
            assertSame(mappedDomain, result.get());
        }

        verify(repository).findByTokenValue("tv");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void revoke_shouldRejectNullTokenValue() {
        RefreshTokenJpaAdapter adapter = new RefreshTokenJpaAdapter(repository);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.revoke(null));
        assertEquals("tokenValue must not be null", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void revoke_shouldDoNothing_whenTokenNotFound() {
        RefreshTokenJpaAdapter adapter = new RefreshTokenJpaAdapter(repository);

        when(repository.findByTokenValue("missing")).thenReturn(Optional.empty());

        adapter.revoke("missing");

        verify(repository).findByTokenValue("missing");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void revoke_shouldSetRevokedAndSave_whenFoundAndNotRevoked() {
        RefreshTokenJpaAdapter adapter = new RefreshTokenJpaAdapter(repository);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"));
        entity.setTokenValue("tv");
        entity.setRevoked(false);

        when(repository.findByTokenValue("tv")).thenReturn(Optional.of(entity));
        when(repository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        adapter.revoke("tv");

        assertTrue(entity.isRevoked());
        verify(repository).findByTokenValue("tv");
        verify(repository).save(same(entity));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void revoke_shouldNotSave_whenFoundButAlreadyRevoked() {
        RefreshTokenJpaAdapter adapter = new RefreshTokenJpaAdapter(repository);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        entity.setTokenValue("tv2");
        entity.setRevoked(true);

        when(repository.findByTokenValue("tv2")).thenReturn(Optional.of(entity));

        adapter.revoke("tv2");

        assertTrue(entity.isRevoked());
        verify(repository).findByTokenValue("tv2");
        verifyNoMoreInteractions(repository);
    }
}
