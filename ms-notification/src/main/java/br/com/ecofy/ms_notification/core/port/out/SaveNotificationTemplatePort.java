package br.com.ecofy.ms_notification.core.port.out;

import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;

public interface SaveNotificationTemplatePort {

    NotificationTemplate save(NotificationTemplate template);
}
