package br.com.ecofy.ms_users.core.application.command;

import java.util.Map;
import java.util.UUID;

public record CreateConnectionCommand(
        UUID userId,
        String type,
        String provider,
        Map<String, Object> metadata,
        String idempotencyKey
) {}