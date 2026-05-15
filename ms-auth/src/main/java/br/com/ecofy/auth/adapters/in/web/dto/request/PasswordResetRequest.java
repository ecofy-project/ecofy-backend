package br.com.ecofy.auth.adapters.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(

        @Email
        @NotBlank
        String email

) {}
