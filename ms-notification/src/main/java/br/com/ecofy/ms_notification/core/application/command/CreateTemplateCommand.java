package br.com.ecofy.ms_notification.core.application.command;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;

import java.util.UUID;

/**
 * Comando de aplicação para criação de template (Dia 7 / item #6).
 * ownerUserId pode ser null (template global).
 */
public record CreateTemplateCommand(
        UUID ownerUserId,
        DomainEventType eventType,
        NotificationChannel channel,
        TemplateEngine engine,
        String subjectTemplate,
        String bodyTemplate,
        boolean active
) { }
