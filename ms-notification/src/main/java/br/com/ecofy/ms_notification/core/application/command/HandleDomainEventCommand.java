package br.com.ecofy.ms_notification.core.application.command;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;

import java.util.Map;
import java.util.UUID;

public record HandleDomainEventCommand(

        DomainEventType eventType,

        UUID userId,

        Map<String, Object> payload,

        String idempotencyKey

) { }