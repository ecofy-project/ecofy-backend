package br.com.ecofy.ms_notification.core.port.in;

import br.com.ecofy.ms_notification.core.application.command.SendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.result.NotificationResult;

public interface SendNotificationUseCase {
    NotificationResult send(SendNotificationCommand command);
}