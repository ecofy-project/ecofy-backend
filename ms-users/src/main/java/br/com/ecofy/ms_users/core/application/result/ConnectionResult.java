package br.com.ecofy.ms_users.core.application.result;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConnectionResult(
        UUID id,
        UUID userId,
        String type,
        String provider,
        Map<String, Object> metadata,
        Instant createdAt
) {}
