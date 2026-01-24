package br.com.ecofy.ms_users.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateConnectionRequest(
        @NotNull UUID userId,
        @NotBlank String type,
        @NotBlank String provider,
        Map<String, Object> metadata
) {}