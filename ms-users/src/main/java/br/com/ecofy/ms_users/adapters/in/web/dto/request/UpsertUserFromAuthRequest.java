package br.com.ecofy.ms_users.adapters.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpsertUserFromAuthRequest(
        @NotBlank String authUserId,
        @Email @NotBlank String email,
        String firstName,
        String lastName,
        String fullName,
        Boolean emailVerified,
        String status,
        String locale
) {}