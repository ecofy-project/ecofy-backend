package br.com.ecofy.ms_notification.core.application.command;

import java.util.UUID;

public record ResendNotificationCommand(

        UUID notificationId,

        String idempotencyKey

) { }