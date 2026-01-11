package br.com.ecofy.ms_users.core.application.command;

import java.util.UUID;

public record LinkAccountCommand(
        UUID userId,
        String provider,
        String externalAccountRef,
        boolean active,
        String idempotencyKey
) {}