package br.com.ecofy.auth.adapters.in.web.dto.response;

import java.time.Instant;
import java.util.Set;

public record UserResponse(

        String id,
        String email,
        String fullName,
        String status,
        boolean emailVerified,
        Set<String> roles,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt

) { }

