package br.com.ecofy.ms_users.adapters.in.web.dto;

public record UpdateProfileRequest(
        String fullName,
        String email,
        String phone,
        String status
) {}
