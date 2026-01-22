package br.com.ecofy.ms_notification.adapters.in.web.dto.response;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;

import java.time.Instant;
import java.util.UUID;

public record TemplateResponse(

        UUID id,

        UUID ownerUserId,

        DomainEventType eventType,

        NotificationChannel channel,

        TemplateEngine engine,

        String subjectTemplate,

        String bodyTemplate,

        boolean active,

        Instant createdAt,

        Instant updatedAt

) { }