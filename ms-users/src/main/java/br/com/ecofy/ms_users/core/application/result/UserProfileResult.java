package br.com.ecofy.ms_users.core.application.result;

import br.com.ecofy.ms_users.core.domain.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResult(

        UUID id,

        String externalAuthId,

        String fullName,

        String email,

        String phone,

        UserStatus status,

        Instant createdAt,

        Instant updatedAt

) { }