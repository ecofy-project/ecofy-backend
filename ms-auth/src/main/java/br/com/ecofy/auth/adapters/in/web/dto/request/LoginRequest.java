package br.com.ecofy.auth.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank
        String clientId,

        String clientSecret,

        @NotBlank
        String username,

        @NotBlank
        String password,

        String scope

) {}