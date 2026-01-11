package br.com.ecofy.ms_users.adapters.in.web.dto;

import java.util.Map;
import java.util.UUID;

public record UserPreferencesResponse(
        UUID userId,
        Map<String, String> preferences
) {}