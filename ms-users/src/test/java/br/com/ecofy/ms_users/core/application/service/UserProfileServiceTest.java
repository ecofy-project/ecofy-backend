package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.config.UsersProperties;
import br.com.ecofy.ms_users.core.application.command.CreateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.command.UpdateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.out.IdempotencyOutcome;
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import br.com.ecofy.ms_users.core.port.out.PublishUserEventPort;
import br.com.ecofy.ms_users.core.port.out.SaveUserProfilePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private SaveUserProfilePort savePort;
    @Mock private LoadUserProfilePort loadPort;
    @Mock private PublishUserEventPort publishPort;
    @Mock private IdempotencyPort idempotencyPort;

    private final UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private UserProfileService service() {
        UsersProperties props = new UsersProperties(
                null, new UsersProperties.Idempotency(Duration.ofHours(1)), null, null);
        return new UserProfileService(savePort, loadPort, publishPort, idempotencyPort, props);
    }

    private EcoUserProfile existing() {
        return EcoUserProfile.builder()
                .id(UserId.of(userId))
                .externalAuthId(ExternalAuthId.of("auth-1"))
                .email(EmailAddress.of("user@ecofy.com"))
                .fullName("User Name")
                .status(UserStatus.PENDING)
                .emailVerified(false)
                .locale("pt-BR")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void create_shouldPersistAndPublish_whenRegistered() {
        var service = service();
        when(idempotencyPort.registerOnce(anyString(), anyString(), anyString(), any()))
                .thenReturn(IdempotencyOutcome.REGISTERED);
        when(loadPort.findById(userId)).thenReturn(Optional.empty());
        when(savePort.save(any(EcoUserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CreateUserProfileCommand(userId, "auth-1", "User Name", "user@ecofy.com", null, "idem-key-1");
        UserProfileResult result = service.create(cmd);

        verify(savePort).save(any(EcoUserProfile.class));
        verify(publishPort).publishUserProfileCreated(eq(userId.toString()), any());
        assertEquals("auth-1", result.externalAuthId());
    }

    @Test
    void create_shouldThrowIdempotencyViolation_onConflict() {
        var service = service();
        when(idempotencyPort.registerOnce(anyString(), anyString(), anyString(), any()))
                .thenReturn(IdempotencyOutcome.CONFLICT);

        var cmd = new CreateUserProfileCommand(userId, "auth-1", "User Name", "user@ecofy.com", null, "idem-key-1");

        assertThrows(IdempotencyViolationException.class, () -> service.create(cmd));
        verify(savePort, never()).save(any());
    }

    @Test
    void update_shouldPersistAndPublish_whenRegistered() {
        var service = service();
        when(idempotencyPort.registerOnce(anyString(), anyString(), anyString(), any()))
                .thenReturn(IdempotencyOutcome.REGISTERED);
        when(loadPort.findById(userId)).thenReturn(Optional.of(existing()));
        when(savePort.save(any(EcoUserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new UpdateUserProfileCommand(userId, "New Name", null, null, "ACTIVE", "idem-key-2");
        UserProfileResult result = service.update(cmd);

        verify(savePort).save(any(EcoUserProfile.class));
        verify(publishPort).publishUserProfileUpdated(eq(userId.toString()), any());
        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals("New Name", result.fullName());
    }

    @Test
    void update_shouldReturnCurrentWithoutReapplying_onDuplicateRetry() {
        var service = service();
        when(idempotencyPort.registerOnce(anyString(), anyString(), anyString(), any()))
                .thenReturn(IdempotencyOutcome.DUPLICATE);
        when(loadPort.findById(userId)).thenReturn(Optional.of(existing()));

        var cmd = new UpdateUserProfileCommand(userId, "New Name", null, null, "ACTIVE", "idem-key-2");
        UserProfileResult result = service.update(cmd);

        // Retry legítimo: não reaplica nem republica.
        verify(savePort, never()).save(any());
        verify(publishPort, never()).publishUserProfileUpdated(anyString(), any());
        assertEquals(userId, result.id());
    }
}
