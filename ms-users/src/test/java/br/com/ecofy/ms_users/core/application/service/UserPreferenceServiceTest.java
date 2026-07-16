package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.config.UsersProperties;
import br.com.ecofy.ms_users.core.application.command.UpdatePreferencesCommand;
import br.com.ecofy.ms_users.core.application.result.UserPreferencesResult;
import br.com.ecofy.ms_users.core.domain.UserPreference;
import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import br.com.ecofy.ms_users.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_users.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.out.IdempotencyOutcome;
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_users.core.port.out.LoadUserPreferencesPort;
import br.com.ecofy.ms_users.core.port.out.SaveUserPreferencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock private SaveUserPreferencePort savePort;
    @Mock private LoadUserPreferencesPort loadPort;
    @Mock private IdempotencyPort idempotencyPort;

    private final UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private UserPreferenceService service() {
        UsersProperties props = new UsersProperties(
                null,
                new UsersProperties.Idempotency(Duration.ofHours(1)),
                null,
                null
        );
        return new UserPreferenceService(savePort, loadPort, idempotencyPort, props);
    }

    private UserPreference pref(PreferenceKey key, String value) {
        return UserPreference.builder()
                .id(UUID.randomUUID())
                .userId(UserId.of(userId))
                .key(key)
                .value(value)
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void update_shouldUpsertValidPreference_andNormalizeValue() {
        var service = service();
        when(idempotencyPort.registerOnce(anyString(), anyString(), anyString(), any()))
                .thenReturn(IdempotencyOutcome.REGISTERED);
        when(savePort.upsertAll(anyList())).thenReturn(List.of(pref(PreferenceKey.THEME, "DARK")));
        when(loadPort.findByUserId(userId)).thenReturn(List.of(pref(PreferenceKey.THEME, "DARK")));

        var cmd = new UpdatePreferencesCommand(userId, Map.of(PreferenceKey.THEME, "dark"), "idem-key-123");
        UserPreferencesResult result = service.update(cmd);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserPreference>> upsertCaptor = ArgumentCaptor.forClass(List.class);
        verify(savePort).upsertAll(upsertCaptor.capture());
        assertEquals(1, upsertCaptor.getValue().size());
        assertEquals("DARK", upsertCaptor.getValue().get(0).getValue()); // THEME normalizado p/ uppercase

        assertEquals("DARK", result.preferences().get("THEME"));
    }

    @Test
    void update_shouldClearPreference_whenValueIsBlank_byDeleting() {
        var service = service();
        when(idempotencyPort.registerOnce(anyString(), anyString(), anyString(), any()))
                .thenReturn(IdempotencyOutcome.REGISTERED);
        when(savePort.upsertAll(anyList())).thenReturn(List.of());
        when(loadPort.findByUserId(userId)).thenReturn(List.of());

        var cmd = new UpdatePreferencesCommand(userId, Map.of(PreferenceKey.THEME, "   "), "idem-key-123");
        service.update(cmd);

        // Nada para upsert; THEME deve ser REMOVIDA (política: valor vazio => remoção).
        verify(savePort).upsertAll(List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<PreferenceKey>> clearCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(savePort).deleteByUserIdAndKeys(any(UserId.class), clearCaptor.capture());
        assertTrue(clearCaptor.getValue().contains(PreferenceKey.THEME));
    }

    @Test
    void update_shouldThrowBusinessValidation_whenValueIsInvalid() {
        var service = service();

        // DEFAULT_CURRENCY inválido (deve ser 3 letras). Falha na normalização, antes da idempotência.
        var cmd = new UpdatePreferencesCommand(userId, Map.of(PreferenceKey.DEFAULT_CURRENCY, "US"), "idem-key-123");

        assertThrows(BusinessValidationException.class, () -> service.update(cmd));
        verifyNoInteractions(idempotencyPort, savePort);
    }

    @Test
    void update_shouldThrowIdempotencyViolation_onConflict() {
        var service = service();
        when(idempotencyPort.registerOnce(anyString(), anyString(), anyString(), any()))
                .thenReturn(IdempotencyOutcome.CONFLICT);

        var cmd = new UpdatePreferencesCommand(userId, Map.of(PreferenceKey.THEME, "dark"), "idem-key-123");

        assertThrows(IdempotencyViolationException.class, () -> service.update(cmd));
        verify(savePort, never()).upsertAll(anyList());
        verify(savePort, never()).deleteByUserIdAndKeys(any(), any());
    }

    @Test
    void update_shouldReturnCurrentStateWithoutReapplying_onDuplicateRetry() {
        var service = service();
        when(idempotencyPort.registerOnce(anyString(), anyString(), anyString(), any()))
                .thenReturn(IdempotencyOutcome.DUPLICATE);
        when(loadPort.findByUserId(userId)).thenReturn(List.of(pref(PreferenceKey.THEME, "DARK")));

        var cmd = new UpdatePreferencesCommand(userId, Map.of(PreferenceKey.THEME, "dark"), "idem-key-123");
        UserPreferencesResult result = service.update(cmd);

        // Retry legítimo: não reaplica (sem upsert/delete), só retorna o estado atual.
        verify(savePort, never()).upsertAll(anyList());
        verify(savePort, never()).deleteByUserIdAndKeys(any(), any());
        assertEquals("DARK", result.preferences().get("THEME"));
    }
}
