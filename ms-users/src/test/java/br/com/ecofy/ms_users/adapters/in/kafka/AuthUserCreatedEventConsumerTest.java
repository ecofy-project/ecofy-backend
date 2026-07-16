package br.com.ecofy.ms_users.adapters.in.kafka;

import br.com.ecofy.ms_users.adapters.in.kafka.dto.AuthUserCreatedEventMessage;
import br.com.ecofy.ms_users.core.application.service.AuthUserSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserCreatedEventConsumerTest {

    @Mock
    private AuthUserSyncService authUserSyncService;

    private AuthUserCreatedEventMessage message(UUID userId) {
        return new AuthUserCreatedEventMessage(userId, "auth-1", "Full Name", "user@ecofy.com", "+5511999999999", null);
    }

    @Test
    void consume_shouldDelegateToSyncService() {
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService);
        UUID userId = UUID.randomUUID();

        consumer.consume(message(userId), "auth.user.created", 0, 10L, 123L);

        verify(authUserSyncService).onAuthUserCreated(
                userId, "auth-1", "Full Name", "user@ecofy.com", "+5511999999999");
    }

    @Test
    void consume_shouldRethrow_whenSyncFails_forRetryDlt() {
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService);
        UUID userId = UUID.randomUUID();

        doThrow(new RuntimeException("db down"))
                .when(authUserSyncService)
                .onAuthUserCreated(any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class,
                () -> consumer.consume(message(userId), "auth.user.created", 0, 10L, 123L));
    }
}
