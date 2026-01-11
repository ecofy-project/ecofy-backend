package br.com.ecofy.ms_users.core.application.command;

import java.util.UUID;

public record UpdateUserProfileCommand(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String status,
        String idempotencyKey
) {}