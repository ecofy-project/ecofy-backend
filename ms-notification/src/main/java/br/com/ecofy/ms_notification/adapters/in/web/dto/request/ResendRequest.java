package br.com.ecofy.ms_notification.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResendRequest(

        @NotNull UUID notificationId,

        String idempotencyKey

) { }