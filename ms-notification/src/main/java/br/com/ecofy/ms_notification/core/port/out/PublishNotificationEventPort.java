package br.com.ecofy.ms_notification.core.port.out;

import br.com.ecofy.ms_notification.core.domain.Notification;

public interface PublishNotificationEventPort {
    void publish(Notification notification);
}
