package br.com.ecofy.ms_notification.core.port.in;

import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;

import java.util.Optional;

public interface GetTemplateUseCase {

    Optional<NotificationTemplate> findById(TemplateId id);
}
