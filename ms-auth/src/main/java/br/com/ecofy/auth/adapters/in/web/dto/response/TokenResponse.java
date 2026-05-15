package br.com.ecofy.auth.adapters.in.web.dto.response;

public record TokenResponse(

        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn

) {}
