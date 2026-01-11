package br.com.ecofy.ms_notification.core.port.in;

import br.com.ecofy.ms_notification.core.application.command.ResendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.result.NotificationResult;

public interface ResendNotificationUseCase {
    NotificationResult resend(ResendNotificationCommand command);
}