package br.com.ecofy.ms_users.adapters.in.web.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String externalAuthId,
        String fullName,
        String email,
        String phone,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
