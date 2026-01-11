package br.com.ecofy.ms_users.core.application.command;

import java.util.UUID;

public record CreateUserProfileCommand(
        UUID userId,
        String externalAuthId,
        String fullName,
        String email,
        String phone,
        String idempotencyKey
) {}