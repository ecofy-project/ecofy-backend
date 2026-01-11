package br.com.ecofy.ms_users.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateProfileRequest(
        @NotNull UUID userId,
        @NotBlank String externalAuthId,
        String fullName,
        String email,
        String phone
) {}