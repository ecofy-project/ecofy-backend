package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.command.CreateTemplateCommand;
import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;
import br.com.ecofy.ms_notification.core.port.out.LoadNotificationTemplatePort;
import br.com.ecofy.ms_notification.core.port.out.SaveNotificationTemplatePort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TemplateManagementServiceTest {

    private final SaveNotificationTemplatePort savePort = mock(SaveNotificationTemplatePort.class);
    private final LoadNotificationTemplatePort loadPort = mock(LoadNotificationTemplatePort.class);
    private final TemplateManagementService service = new TemplateManagementService(savePort, loadPort);

    @Test
    void create_shouldBuildValidateAndPersistTemplate() {
        when(savePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new CreateTemplateCommand(
                UUID.randomUUID(), DomainEventType.BUDGET_ALERT, NotificationChannel.EMAIL,
                TemplateEngine.SIMPLE, "Assunto", "Corpo", true);

        NotificationTemplate created = service.create(cmd);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        ArgumentCaptor<NotificationTemplate> captor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(savePort).save(captor.capture());
        assertThat(captor.getValue().getBodyTemplate()).isEqualTo("Corpo");
    }

    @Test
    void create_shouldRejectEmailTemplateWithoutSubject() {
        var cmd = new CreateTemplateCommand(
                null, DomainEventType.BUDGET_ALERT, NotificationChannel.EMAIL,
                TemplateEngine.SIMPLE, null, "Corpo", true);

        assertThatThrownBy(() -> service.create(cmd)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(savePort);
    }

    @Test
    void findById_shouldDelegateToLoadPort() {
        var id = TemplateId.newId();
        when(loadPort.loadById(id)).thenReturn(Optional.empty());

        assertThat(service.findById(id)).isEmpty();
        verify(loadPort).loadById(id);
    }
}
