package br.com.ecofy.ms_users.adapters.in.web.dto.request;

public record UpdateProfileRequest(
        String fullName,
        String email,
        String phone,
        String status
) {}
