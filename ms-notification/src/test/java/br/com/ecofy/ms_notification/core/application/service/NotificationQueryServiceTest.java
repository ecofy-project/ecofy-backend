package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.port.out.ListNotificationsPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NotificationQueryServiceTest {

    private final ListNotificationsPort port = mock(ListNotificationsPort.class);
    private final NotificationQueryService service = new NotificationQueryService(port);

    @Test
    void listByUser_shouldApplyDefaultLimit_whenInvalid() {
        UUID userId = UUID.randomUUID();
        when(port.listByUser(eq(userId), anyInt())).thenReturn(List.of());

        service.listByUser(userId, 0);

        verify(port).listByUser(userId, 50); // default
    }

    @Test
    void listByUser_shouldClampToMax() {
        UUID userId = UUID.randomUUID();
        when(port.listByUser(eq(userId), anyInt())).thenReturn(List.of());

        service.listByUser(userId, 9999);

        verify(port).listByUser(userId, 200); // max
    }

    @Test
    void listByUser_shouldPassThroughValidLimit() {
        UUID userId = UUID.randomUUID();
        when(port.listByUser(eq(userId), anyInt())).thenReturn(List.of());

        service.listByUser(userId, 25);

        verify(port).listByUser(userId, 25);
    }
}
