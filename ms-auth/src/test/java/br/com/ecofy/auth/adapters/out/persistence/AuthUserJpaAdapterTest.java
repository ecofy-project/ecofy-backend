package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.AuthUserEntity;
import br.com.ecofy.auth.adapters.out.persistence.mapper.PersistenceMapper;
import br.com.ecofy.auth.adapters.out.persistence.repository.AuthUserRepository;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserJpaAdapterTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Test
    void constructor_shouldRejectNullRepository() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new AuthUserJpaAdapter(null));
        assertEquals("authUserRepository must not be null", ex.getMessage());
    }

    @Test
    void save_shouldRejectNullUser() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.save(null));
        assertEquals("user must not be null", ex.getMessage());

        verifyNoInteractions(authUserRepository);
    }

    @Test
    void save_shouldCreateNewEntity_whenNotFound_andSetCreatedAtAndUpdatedAt() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AuthUser user = mockAuthUser(id, "user@ecofy.com");

        when(authUserRepository.findById(id)).thenReturn(Optional.empty());

        ArgumentCaptor<AuthUserEntity> entityCaptor = ArgumentCaptor.forClass(AuthUserEntity.class);
        when(authUserRepository.save(entityCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        AuthUser mappedDomain = mock(AuthUser.class);
        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toDomain(any(AuthUserEntity.class), any(), any()))
                    .thenReturn(mappedDomain);

            AuthUser result = adapter.save(user);
            assertSame(mappedDomain, result);
        }

        AuthUserEntity savedEntity = entityCaptor.getValue();
        assertNotNull(savedEntity);
        assertEquals(id, savedEntity.getId());
        assertEquals("user@ecofy.com", savedEntity.getEmail());
        assertNotNull(savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getUpdatedAt());
        assertFalse(savedEntity.getUpdatedAt().isBefore(savedEntity.getCreatedAt()));

        verify(authUserRepository).findById(id);
        verify(authUserRepository).save(any(AuthUserEntity.class));
        verifyNoMoreInteractions(authUserRepository);
    }

    @Test
    void save_shouldUpdateExistingEntity_whenFound_andKeepExistingCreatedAt_whenAlreadySet() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        AuthUser user = mockAuthUser(id, "existing@ecofy.com");

        Instant existingCreatedAt = Instant.parse("2020-01-01T00:00:00Z");
        AuthUserEntity existing = new AuthUserEntity();
        existing.setId(id);
        existing.setCreatedAt(existingCreatedAt);

        when(authUserRepository.findById(id)).thenReturn(Optional.of(existing));

        ArgumentCaptor<AuthUserEntity> entityCaptor = ArgumentCaptor.forClass(AuthUserEntity.class);
        when(authUserRepository.save(entityCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        AuthUser mappedDomain = mock(AuthUser.class);
        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toDomain(same(existing), any(), any()))
                    .thenReturn(mappedDomain);

            AuthUser result = adapter.save(user);
            assertSame(mappedDomain, result);
        }

        AuthUserEntity savedEntity = entityCaptor.getValue();
        assertSame(existing, savedEntity);
        assertEquals(existingCreatedAt, savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getUpdatedAt());
        assertEquals("existing@ecofy.com", savedEntity.getEmail());

        verify(authUserRepository).findById(id);
        verify(authUserRepository).save(existing);
        verifyNoMoreInteractions(authUserRepository);
    }

    @Test
    void save_shouldSetCreatedAt_whenExistingEntityHasNullCreatedAt() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        AuthUser user = mockAuthUser(id, "nullcreated@ecofy.com");

        AuthUserEntity existing = new AuthUserEntity();
        existing.setId(id);
        existing.setCreatedAt(null);

        when(authUserRepository.findById(id)).thenReturn(Optional.of(existing));

        ArgumentCaptor<AuthUserEntity> entityCaptor = ArgumentCaptor.forClass(AuthUserEntity.class);
        when(authUserRepository.save(entityCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        AuthUser mappedDomain = mock(AuthUser.class);
        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toDomain(same(existing), any(), any()))
                    .thenReturn(mappedDomain);

            AuthUser result = adapter.save(user);
            assertSame(mappedDomain, result);
        }

        AuthUserEntity savedEntity = entityCaptor.getValue();
        assertSame(existing, savedEntity);
        assertNotNull(savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getUpdatedAt());

        verify(authUserRepository).findById(id);
        verify(authUserRepository).save(existing);
        verifyNoMoreInteractions(authUserRepository);
    }

    @Test
    void loadByEmail_shouldRejectNullEmail() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.loadByEmail(null));
        assertEquals("email must not be null", ex.getMessage());

        verifyNoInteractions(authUserRepository);
    }

    @Test
    void loadByEmail_shouldReturnEmpty_whenNotFound() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("missing@ecofy.com");
        when(authUserRepository.findByEmailIgnoreCase("missing@ecofy.com")).thenReturn(Optional.empty());

        Optional<AuthUser> result = adapter.loadByEmail(email);

        assertTrue(result.isEmpty());
        verify(authUserRepository).findByEmailIgnoreCase("missing@ecofy.com");
        verifyNoMoreInteractions(authUserRepository);
    }

    @Test
    void loadByEmail_shouldMapToDomain_whenFound() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("found@ecofy.com");

        AuthUserEntity entity = new AuthUserEntity();
        entity.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        entity.setEmail("found@ecofy.com");

        when(authUserRepository.findByEmailIgnoreCase("found@ecofy.com")).thenReturn(Optional.of(entity));

        AuthUser mappedDomain = mock(AuthUser.class);
        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toDomain(same(entity), any(), any()))
                    .thenReturn(mappedDomain);

            Optional<AuthUser> result = adapter.loadByEmail(email);

            assertTrue(result.isPresent());
            assertSame(mappedDomain, result.get());
        }

        verify(authUserRepository).findByEmailIgnoreCase("found@ecofy.com");
        verifyNoMoreInteractions(authUserRepository);
    }

    @Test
    void loadById_shouldRejectNullId() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.loadById(null));
        assertEquals("id must not be null", ex.getMessage());

        verifyNoInteractions(authUserRepository);
    }

    @Test
    void loadById_shouldReturnEmpty_whenNotFound() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        UUID uuid = UUID.fromString("55555555-5555-5555-5555-555555555555");
        AuthUserId id = mock(AuthUserId.class);
        when(id.value()).thenReturn(uuid);
        when(authUserRepository.findById(uuid)).thenReturn(Optional.empty());

        Optional<AuthUser> result = adapter.loadById(id);

        assertTrue(result.isEmpty());
        verify(authUserRepository).findById(uuid);
        verifyNoMoreInteractions(authUserRepository);
    }

    @Test
    void loadById_shouldMapToDomain_whenFound() {
        AuthUserJpaAdapter adapter = new AuthUserJpaAdapter(authUserRepository);

        UUID uuid = UUID.fromString("66666666-6666-6666-6666-666666666666");
        AuthUserId id = mock(AuthUserId.class);
        when(id.value()).thenReturn(uuid);

        AuthUserEntity entity = new AuthUserEntity();
        entity.setId(uuid);
        entity.setEmail("byid@ecofy.com");

        when(authUserRepository.findById(uuid)).thenReturn(Optional.of(entity));

        AuthUser mappedDomain = mock(AuthUser.class);
        try (MockedStatic<PersistenceMapper> mocked = mockStatic(PersistenceMapper.class)) {
            mocked.when(() -> PersistenceMapper.toDomain(same(entity), any(), any()))
                    .thenReturn(mappedDomain);

            Optional<AuthUser> result = adapter.loadById(id);

            assertTrue(result.isPresent());
            assertSame(mappedDomain, result.get());
        }

        verify(authUserRepository).findById(uuid);
        verifyNoMoreInteractions(authUserRepository);
    }

    // heapers

    private static AuthUser mockAuthUser(UUID id, String emailValue) {
        AuthUser user = mock(AuthUser.class);

        AuthUserId authUserId = mock(AuthUserId.class);
        when(authUserId.value()).thenReturn(id);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn(emailValue);

        PasswordHash passwordHash = mock(PasswordHash.class);
        when(passwordHash.value()).thenReturn("hash");

        when(user.id()).thenReturn(authUserId);
        when(user.email()).thenReturn(email);
        when(user.passwordHash()).thenReturn(passwordHash);

        return user;
    }
}