package br.com.ecofy.ms_users.core.application.result;

import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
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

) {

    // Converte o agregado de domínio EcoUserProfile em um DTO/Result estável (tipos simples),
    // evitando expor Value Objects para fora da camada de aplicação.
    public static UserProfileResult from(EcoUserProfile p) {
        if (p == null) return null;

        UUID id = (p.getId() != null) ? p.getId().value() : null;
        String externalAuthId = (p.getExternalAuthId() != null) ? p.getExternalAuthId().value() : null;
        String email = (p.getEmail() != null) ? p.getEmail().value() : null;
        String phone = (p.getPhone() != null) ? p.getPhone().value() : null;

        return new UserProfileResult(
                id,
                externalAuthId,
                p.getFullName(),
                email,
                phone,
                p.getStatus(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
