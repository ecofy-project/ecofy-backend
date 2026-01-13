package br.com.ecofy.ms_users.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.Map;

public record EcoUserEvent(
        String type,
        String key,
        Instant occurredAt,
        Map<String, Object> payload
) {}
