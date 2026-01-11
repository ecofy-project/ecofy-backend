package br.com.ecofy.ms_users.adapters.in.web.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConnectionResponse(
        UUID id,
        UUID userId,
        String type,
        String provider,
        Map<String, Object> metadata,
        Instant createdAt
) {}