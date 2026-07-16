package br.com.ecofy.ms_notification.adapters.out.messaging;

import br.com.ecofy.ms_notification.adapters.out.messaging.dto.NotificationSentEvent;
import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.domain.Notification;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationStatus;
import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationEventsKafkaAdapterTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

    private NotificationProperties props() {
        var props = new NotificationProperties();
        props.getTopics().setNotificationSent("eco.notification.sent");
        return props;
    }

    private Notification sentNotification(UUID notificationId, UUID userId) {
        var now = Instant.now();
        return Notification.builder()
                .id(new NotificationId(notificationId))
                .userId(new UserId(userId))
                .eventType(DomainEventType.BUDGET_ALERT)
                .channel(NotificationChannel.EMAIL)
                .destination(new ChannelAddress(NotificationChannel.EMAIL, "user@example.com"))
                .subject("s").body("b")
                .status(NotificationStatus.SENT)
                .attemptCount(1)
                .payload(Map.of())
                .createdAt(now).updatedAt(now)
                .build();
    }

    @Test
    void publish_shouldMapDomainToEventAndSendWithNotificationIdKey() {
        var adapter = new NotificationEventsKafkaAdapter(kafkaTemplate, props());
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        adapter.publish(sentNotification(notificationId, userId));

        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("eco.notification.sent"), eq(notificationId.toString()), valueCaptor.capture());

        assertThat(valueCaptor.getValue()).isInstanceOf(NotificationSentEvent.class);
        NotificationSentEvent event = (NotificationSentEvent) valueCaptor.getValue();
        assertThat(event.notificationId()).isEqualTo(notificationId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.status()).isEqualTo("SENT");
        assertThat(event.channel()).isEqualTo("EMAIL");
    }
}
