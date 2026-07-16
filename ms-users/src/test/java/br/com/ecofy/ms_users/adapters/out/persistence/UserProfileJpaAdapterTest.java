package br.com.ecofy.ms_users.adapters.out.persistence;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.UserProfileEntity;
import br.com.ecofy.ms_users.adapters.out.persistence.mapper.UserProfileMapper;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.UserProfileRepository;
import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileJpaAdapterTest {

    @Mock
    private UserProfileRepository repo;

    private final UserProfileMapper mapper = new UserProfileMapper();

    private UserProfileJpaAdapter adapter() {
        return new UserProfileJpaAdapter(repo, mapper);
    }

    private UserProfileEntity entity(String externalAuthId, boolean emailVerified, String locale) {
        var e = new UserProfileEntity();
        e.setId(UUID.randomUUID());
        e.setExternalAuthId(externalAuthId);
        e.setEmail("user@ecofy.com");
        e.setFullName("User Name");
        e.setStatus(UserStatus.ACTIVE);
        e.setEmailVerified(emailVerified);
        e.setLocale(locale);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return e;
    }

    @Test
    void findByExternalAuthId_shouldQueryWithRealValue_notValueObjectToString() {
        var adapter = adapter();
        when(repo.findByExternalAuthId("auth-123"))
                .thenReturn(Optional.of(entity("auth-123", true, "pt-BR")));

        Optional<EcoUserProfile> found = adapter.findByExternalAuthId(ExternalAuthId.of("auth-123"));

        assertTrue(found.isPresent());
        assertEquals("auth-123", found.get().getExternalAuthId().value());

        // Deve consultar pelo VALOR real, nunca por "ExternalAuthId[value=auth-123]".
        verify(repo).findByExternalAuthId("auth-123");
        verify(repo, never()).findByExternalAuthId(contains("ExternalAuthId"));
    }

    @Test
    void findByExternalAuthId_shouldReturnEmpty_whenNull() {
        var adapter = adapter();

        Optional<EcoUserProfile> found = adapter.findByExternalAuthId(null);

        assertTrue(found.isEmpty());
        verifyNoInteractions(repo);
    }

    @Test
    void save_shouldRoundTripEmailVerifiedAndLocale() {
        var adapter = adapter();
        when(repo.save(any(UserProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        EcoUserProfile domain = EcoUserProfile.builder()
                .id(UserId.of(UUID.randomUUID()))
                .externalAuthId(ExternalAuthId.of("auth-xyz"))
                .email(EmailAddress.of("user@ecofy.com"))
                .fullName("User Name")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .locale("en-US")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        EcoUserProfile saved = adapter.save(domain);

        // Campos preservados no domínio retornado (não são perdidos no save/load).
        assertTrue(saved.isEmailVerified());
        assertEquals("en-US", saved.getLocale());

        // E também gravados na entidade persistida.
        ArgumentCaptor<UserProfileEntity> captor = ArgumentCaptor.forClass(UserProfileEntity.class);
        verify(repo).save(captor.capture());
        assertTrue(captor.getValue().isEmailVerified());
        assertEquals("en-US", captor.getValue().getLocale());
        assertEquals("auth-xyz", captor.getValue().getExternalAuthId());
    }
}
