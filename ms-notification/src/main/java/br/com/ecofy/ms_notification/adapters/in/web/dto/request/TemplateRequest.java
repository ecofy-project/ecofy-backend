package br.com.ecofy.ms_notification.adapters.in.web.dto.request;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TemplateRequest(

        UUID ownerUserId,

        @NotNull DomainEventType eventType,

        @NotNull NotificationChannel channel,

        @NotNull TemplateEngine engine,

        @NotBlank String bodyTemplate,

        String subjectTemplate,

        boolean active

) { }
