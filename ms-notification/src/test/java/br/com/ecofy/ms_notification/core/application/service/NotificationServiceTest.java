package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.command.ResendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.command.SendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.config.NotificationSettings;
import br.com.ecofy.ms_notification.core.application.result.NotificationResult;
import br.com.ecofy.ms_notification.core.domain.Notification;
import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationStatus;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;
import br.com.ecofy.ms_notification.core.domain.exception.DeliveryProviderException;
import br.com.ecofy.ms_notification.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_notification.core.domain.exception.TemplateNotFoundException;
import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import br.com.ecofy.ms_notification.core.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private LoadNotificationTemplatePort loadTemplatePort;
    private LoadUserContactInfoPort loadUserContactInfoPort;
    private SaveNotificationPort saveNotificationPort;
    private SaveDeliveryAttemptPort saveDeliveryAttemptPort;
    private EmailSenderPort emailSenderPort;
    private WhatsAppSenderPort whatsAppSenderPort;
    private PushSenderPort pushSenderPort;
    private IdempotencyPort idempotencyPort;
    private PublishNotificationEventPort publishPort;

    private NotificationService service;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        loadTemplatePort = mock(LoadNotificationTemplatePort.class);
        loadUserContactInfoPort = mock(LoadUserContactInfoPort.class);
        saveNotificationPort = mock(SaveNotificationPort.class);
        saveDeliveryAttemptPort = mock(SaveDeliveryAttemptPort.class);
        emailSenderPort = mock(EmailSenderPort.class);
        whatsAppSenderPort = mock(WhatsAppSenderPort.class);
        pushSenderPort = mock(PushSenderPort.class);
        idempotencyPort = mock(IdempotencyPort.class);
        publishPort = mock(PublishNotificationEventPort.class);

        service = build(3);
        // save devolve o próprio objeto persistido
        when(saveNotificationPort.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(idempotencyPort.tryAcquire(any())).thenReturn(true);
    }

    private NotificationService build(int maxAttempts) {
        var settings = new NotificationSettings(true, maxAttempts, Duration.ofSeconds(1), 2.0, Map.of());
        var retry = new RetryPolicyService(settings);
        return new NotificationService(
                settings, loadTemplatePort, loadUserContactInfoPort, saveNotificationPort,
                saveDeliveryAttemptPort, emailSenderPort, whatsAppSenderPort, pushSenderPort,
                idempotencyPort, retry, Optional.of(publishPort)
        );
    }

    private NotificationTemplate emailTemplate() {
        var now = Instant.now();
        return NotificationTemplate.builder()
                .id(TemplateId.newId())
                .ownerUserId(null)
                .eventType(DomainEventType.BUDGET_ALERT)
                .channel(NotificationChannel.EMAIL)
                .engine(TemplateEngine.SIMPLE)
                .subjectTemplate("Alerta")
                .bodyTemplate("Corpo do alerta")
                .active(true)
                .createdAt(now).updatedAt(now)
                .build();
    }

    private SendNotificationCommand emailCommand(String idem) {
        return new SendNotificationCommand(USER_ID, DomainEventType.BUDGET_ALERT, NotificationChannel.EMAIL,
                "user@example.com", Map.of("k", "v"), idem);
    }

    @Test
    void send_shouldDeliverAndPublish_whenTemplateExistsAndProviderSucceeds() {
        when(loadTemplatePort.loadActiveTemplate(any(), eq(DomainEventType.BUDGET_ALERT), eq(NotificationChannel.EMAIL)))
                .thenReturn(Optional.of(emailTemplate()));
        when(emailSenderPort.sendEmail(any(), any(), any()))
                .thenReturn(new EmailSenderPort.SendResult("email-stub", "msg-1"));

        NotificationResult result = service.send(emailCommand("idem-abc-123"));

        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.userId()).isEqualTo(USER_ID); // userId preservado
        assertThat(result.attemptCount()).isEqualTo(1);
        verify(emailSenderPort).sendEmail(any(), eq("Alerta"), eq("Corpo do alerta"));
        verify(saveDeliveryAttemptPort).save(any());
        verify(publishPort).publish(any(Notification.class)); // publica domínio, não DTO
    }

    @Test
    void send_shouldThrowTemplateNotFound_whenNoActiveTemplate() {
        when(loadTemplatePort.loadActiveTemplate(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(emailCommand("idem-abc-123")))
                .isInstanceOf(TemplateNotFoundException.class);

        verifyNoInteractions(emailSenderPort);
    }

    @Test
    void send_shouldThrowIdempotencyViolation_whenKeyAlreadyUsed() {
        when(idempotencyPort.tryAcquire(any())).thenReturn(false);

        assertThatThrownBy(() -> service.send(emailCommand("idem-abc-123")))
                .isInstanceOf(IdempotencyViolationException.class);

        verifyNoInteractions(loadTemplatePort);
    }

    @Test
    void send_shouldRetryUpToMaxAttempts_thenFail_whenProviderKeepsFailing() {
        service = build(3);
        when(saveNotificationPort.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(idempotencyPort.tryAcquire(any())).thenReturn(true);

        when(loadTemplatePort.loadActiveTemplate(any(), any(), any())).thenReturn(Optional.of(emailTemplate()));
        when(emailSenderPort.sendEmail(any(), any(), any())).thenThrow(new RuntimeException("smtp down"));

        assertThatThrownBy(() -> service.send(emailCommand("idem-abc-123")))
                .isInstanceOf(DeliveryProviderException.class);

        // maxAttempts=3 -> 3 tentativas de envio + 3 registros de falha
        verify(emailSenderPort, times(3)).sendEmail(any(), any(), any());
        verify(saveDeliveryAttemptPort, times(3)).save(any());
        verify(publishPort, never()).publish(any(Notification.class));
    }

    @Test
    void send_shouldSucceedOnSecondAttempt_whenProviderFailsThenSucceeds() {
        when(loadTemplatePort.loadActiveTemplate(any(), any(), any())).thenReturn(Optional.of(emailTemplate()));
        when(emailSenderPort.sendEmail(any(), any(), any()))
                .thenThrow(new RuntimeException("transient"))
                .thenReturn(new EmailSenderPort.SendResult("email-stub", "msg-2"));

        NotificationResult result = service.send(emailCommand("idem-abc-123"));

        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.attemptCount()).isEqualTo(2);
        verify(emailSenderPort, times(2)).sendEmail(any(), any(), any());
    }

    @Test
    void resend_shouldReloadResetAndDeliver() {
        UUID notificationId = UUID.randomUUID();
        Notification existing = Notification.builder()
                .id(new NotificationId(notificationId))
                .userId(new UserId(USER_ID))
                .eventType(DomainEventType.BUDGET_ALERT)
                .channel(NotificationChannel.EMAIL)
                .destination(new ChannelAddress(NotificationChannel.EMAIL, "user@example.com"))
                .subject("Alerta").body("Corpo")
                .status(NotificationStatus.FAILED)
                .attemptCount(1)
                .payload(Map.of())
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(saveNotificationPort.loadById(any())).thenReturn(Optional.of(existing));
        when(emailSenderPort.sendEmail(any(), any(), any()))
                .thenReturn(new EmailSenderPort.SendResult("email-stub", "msg-3"));

        NotificationResult result = service.resend(new ResendNotificationCommand(notificationId, "idem-resend-1"));

        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(saveNotificationPort, atLeastOnce()).save(captor.capture());
        assertThat(result.userId()).isEqualTo(USER_ID);
    }
}
