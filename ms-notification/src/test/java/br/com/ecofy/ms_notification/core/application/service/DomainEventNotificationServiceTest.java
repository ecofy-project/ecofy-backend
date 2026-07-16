package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.command.HandleDomainEventCommand;
import br.com.ecofy.ms_notification.core.application.command.SendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.config.NotificationSettings;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.port.in.SendNotificationUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DomainEventNotificationServiceTest {

    private final SendNotificationUseCase sendUseCase = mock(SendNotificationUseCase.class);

    private DomainEventNotificationService service(Map<String, String> defaultChannels) {
        var settings = new NotificationSettings(true, 3, Duration.ofSeconds(1), 2.0, defaultChannels);
        return new DomainEventNotificationService(settings, sendUseCase);
    }

    @Test
    void handle_shouldResolveConfiguredChannelAndDelegateToSend() {
        var service = service(Map.of("BUDGET_ALERT", "PUSH"));
        UUID userId = UUID.randomUUID();

        service.handle(new HandleDomainEventCommand(DomainEventType.BUDGET_ALERT, userId, Map.of("a", 1), "evt-1"));

        ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(sendUseCase).send(captor.capture());
        assertThat(captor.getValue().channel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("evt-1");
    }

    @Test
    void handle_shouldFallbackToEmail_whenNoChannelConfigured() {
        var service = service(Map.of());
        service.handle(new HandleDomainEventCommand(DomainEventType.INSIGHT_CREATED, UUID.randomUUID(), Map.of(), null));

        ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(sendUseCase).send(captor.capture());
        assertThat(captor.getValue().channel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void handle_shouldFallbackToEmail_whenConfiguredChannelIsInvalid() {
        var service = service(Map.of("BUDGET_ALERT", "NOT_A_CHANNEL"));
        service.handle(new HandleDomainEventCommand(DomainEventType.BUDGET_ALERT, UUID.randomUUID(), Map.of(), null));

        ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(sendUseCase).send(captor.capture());
        assertThat(captor.getValue().channel()).isEqualTo(NotificationChannel.EMAIL);
    }
}
