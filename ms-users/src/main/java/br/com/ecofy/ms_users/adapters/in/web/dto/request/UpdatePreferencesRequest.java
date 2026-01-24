package br.com.ecofy.ms_users.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record UpdatePreferencesRequest(
        @NotEmpty Map<String, String> preferences
) {}