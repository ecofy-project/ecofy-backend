package br.com.ecofy.auth.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @NotBlank
        String clientId,

        @NotBlank
        String refreshToken,

        String scope

) {}
