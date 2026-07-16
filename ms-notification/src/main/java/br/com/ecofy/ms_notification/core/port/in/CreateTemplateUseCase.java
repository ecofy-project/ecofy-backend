package br.com.ecofy.ms_notification.core.port.in;

import br.com.ecofy.ms_notification.core.application.command.CreateTemplateCommand;
import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;

public interface CreateTemplateUseCase {

    NotificationTemplate create(CreateTemplateCommand command);
}
