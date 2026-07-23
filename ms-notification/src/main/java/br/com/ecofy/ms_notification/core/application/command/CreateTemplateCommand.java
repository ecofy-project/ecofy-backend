package br.com.ecofy.ms_notification.core.application.command;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;

import java.util.UUID;

// Transporta os dados de criação de um template, aceitando owner nulo para templates globais.
public record CreateTemplateCommand(
        UUID ownerUserId,
        DomainEventType eventType,
        NotificationChannel channel,
        TemplateEngine engine,
        String subjectTemplate,
        String bodyTemplate,
        boolean active
) { }
