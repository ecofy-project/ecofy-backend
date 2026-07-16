package br.com.ecofy.ms_notification.core.domain;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationStatus;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;
import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationDomainTest {

    private Notification.NotificationBuilder base() {
        var now = Instant.now();
        return Notification.builder()
                .id(NotificationId.newId())
                .userId(new UserId(UUID.randomUUID()))
                .eventType(DomainEventType.BUDGET_ALERT)
                .channel(NotificationChannel.EMAIL)
                .destination(new ChannelAddress(NotificationChannel.EMAIL, "user@example.com"))
                .subject("Assunto").body("Corpo")
                .status(NotificationStatus.PENDING)
                .attemptCount(0)
                .payload(Map.of())
                .createdAt(now).updatedAt(now);
    }

    @Test
    void validateForSend_shouldPass_forValidEmailNotification() {
        assertThat(base().build().validateForSend()).isNotNull();
    }

    @Test
    void validateForSend_shouldRequireSubjectForEmail() {
        assertThatThrownBy(() -> base().subject(null).build().validateForSend())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateForSend_shouldRejectBlankBody() {
        assertThatThrownBy(() -> base().body("   ").build().validateForSend())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void channelAddress_shouldRejectBlankAddress() {
        assertThatThrownBy(() -> new ChannelAddress(NotificationChannel.EMAIL, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void template_shouldRenderBodyWithVariables() {
        var now = Instant.now();
        var template = NotificationTemplate.builder()
                .id(TemplateId.newId())
                .eventType(DomainEventType.BUDGET_ALERT)
                .channel(NotificationChannel.EMAIL)
                .engine(TemplateEngine.SIMPLE)
                .subjectTemplate("Alerta de {{categoryId}}")
                .bodyTemplate("Consumo: {{consumedPct}}%")
                .active(true)
                .createdAt(now).updatedAt(now)
                .build()
                .validate();

        String body = template.renderBody(Map.of("consumedPct", 80));
        assertThat(body).contains("80");
    }

    @Test
    void template_shouldRequireSubjectForEmailChannel() {
        var now = Instant.now();
        assertThatThrownBy(() -> NotificationTemplate.builder()
                .id(TemplateId.newId())
                .eventType(DomainEventType.BUDGET_ALERT)
                .channel(NotificationChannel.EMAIL)
                .engine(TemplateEngine.SIMPLE)
                .subjectTemplate(null)
                .bodyTemplate("Corpo")
                .active(true)
                .createdAt(now).updatedAt(now)
                .build()
                .validate())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
