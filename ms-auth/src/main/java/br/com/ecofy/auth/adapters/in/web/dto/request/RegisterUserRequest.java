package br.com.ecofy.auth.adapters.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank String firstName,

        @NotBlank
        String lastName,

        String locale

) {}
