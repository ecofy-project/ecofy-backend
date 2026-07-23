package br.com.ecofy.ms_notification.core.port.out;

import br.com.ecofy.ms_notification.core.application.result.NotificationResult;

import java.util.List;
import java.util.UUID;

public interface ListNotificationsPort {

    List<NotificationResult> listByUser(UUID userId, int limit);
}
