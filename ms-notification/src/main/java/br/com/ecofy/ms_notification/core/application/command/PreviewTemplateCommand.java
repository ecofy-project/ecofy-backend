package br.com.ecofy.ms_notification.core.application.command;


import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;

import java.util.Map;
import java.util.UUID;

public record PreviewTemplateCommand(

        UUID userId,

        DomainEventType eventType,

        NotificationChannel channel,

        Map<String, Object> payload

) { }