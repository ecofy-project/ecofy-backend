package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.in.UpsertUserFromAuthUseCase;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import br.com.ecofy.ms_users.core.port.out.SaveUserProfilePort;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserSyncServiceTest {

    @Mock
    private LoadUserProfilePort loadPort;

    @Mock
    private SaveUserProfilePort savePort;

    private AuthUserSyncService service() {
        return new AuthUserSyncService(loadPort, savePort);
    }

    private UpsertUserFromAuthUseCase.Command command() {
        return new UpsertUserFromAuthUseCase.Command(
                "auth-1", "user@ecofy.com", "First", "Last", null, true, "ACTIVE", "en-US");
    }

    @Test
    void upsert_shouldUpdateExistingProfile_notDuplicate_whenFoundByExternalAuthId() {
        var service = service();
        UUID existingId = UUID.fromString("11111111-2222-3333-4444-555555555555");

        EcoUserProfile existing = EcoUserProfile.builder()
                .id(UserId.of(existingId))
                .externalAuthId(ExternalAuthId.of("auth-1"))
                .email(EmailAddress.of("old@ecofy.com"))
                .fullName("Old Name")
                .status(UserStatus.PENDING)
                .emailVerified(false)
                .locale("pt-BR")
                .createdAt(Instant.now().minusSeconds(60))
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(loadPort.findByExternalAuthId(any(ExternalAuthId.class))).thenReturn(Optional.of(existing));
        when(savePort.save(any(EcoUserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResult result = service.upsert(command());

        // Buscou por externalAuthId e ATUALIZOU o existente (mesmo id) — sem criar duplicado.
        verify(loadPort).findByExternalAuthId(any(ExternalAuthId.class));

        ArgumentCaptor<EcoUserProfile> captor = ArgumentCaptor.forClass(EcoUserProfile.class);
        verify(savePort, times(1)).save(captor.capture());
        assertEquals(existingId, captor.getValue().getId().value(), "Deve manter o id do perfil existente");
        assertTrue(captor.getValue().isEmailVerified(), "emailVerified sincronizado do Auth");
        assertEquals(existingId, result.id());
    }

    @Test
    void upsert_shouldCreateNewProfile_whenNotFound() {
        var service = service();

        when(loadPort.findByExternalAuthId(any(ExternalAuthId.class))).thenReturn(Optional.empty());
        when(savePort.save(any(EcoUserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResult result = service.upsert(command());

        ArgumentCaptor<EcoUserProfile> captor = ArgumentCaptor.forClass(EcoUserProfile.class);
        verify(savePort, times(1)).save(captor.capture());
        assertEquals("auth-1", captor.getValue().getExternalAuthId().value());
        assertEquals("en-US", captor.getValue().getLocale());
        assertNotNull(result.id());
    }
}
