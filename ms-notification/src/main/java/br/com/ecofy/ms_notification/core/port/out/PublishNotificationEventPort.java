package br.com.ecofy.ms_notification.core.port.out;

import br.com.ecofy.ms_notification.adapters.out.messaging.dto.NotificationSentEvent;

public interface PublishNotificationEventPort {
    void publish(NotificationSentEvent event);
}
