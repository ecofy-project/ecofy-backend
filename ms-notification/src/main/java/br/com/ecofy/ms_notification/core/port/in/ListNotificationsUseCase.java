package br.com.ecofy.ms_notification.core.port.in;

import br.com.ecofy.ms_notification.core.application.result.NotificationResult;

import java.util.List;
import java.util.UUID;

public interface ListNotificationsUseCase {
    List<NotificationResult> listByUser(UUID userId, int limit);
}
