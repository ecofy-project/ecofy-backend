package br.com.ecofy.auth.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailRequest(

        @NotBlank
        String token

) {}