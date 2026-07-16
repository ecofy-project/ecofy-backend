package br.com.ecofy.ms_users.adapters.out.persistence.mapper;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.UserProfileEntity;
import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.PhoneNumber;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;

import java.util.Objects;

/**
 * Mapper JPA <-> Domain para EcoUserProfile.
 * Padrão: fail-fast para entidade/domain nulos e helpers para VOs opcionais.
 */
public final class UserProfileMapper {

    // Cria uma instância do mapper (sem estado) para conversões entre Entity e Domain.
    public UserProfileMapper() {
    }

    // Converte UserProfileEntity (persistência) para EcoUserProfile (domínio), incluindo conversão de campos opcionais para Value Objects.
    public EcoUserProfile toDomain(UserProfileEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return EcoUserProfile.builder()
                .id(UserId.of(e.getId()))
                .externalAuthId(toExternalAuthIdOrNull(e.getExternalAuthId()))
                .fullName(e.getFullName())
                .email(toEmailOrNull(e.getEmail()))
                .phone(toPhoneOrNull(e.getPhone()))
                .status(e.getStatus())
                .emailVerified(e.isEmailVerified())
                .locale(e.getLocale())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    // Converte EcoUserProfile (domínio) para UserProfileEntity (persistência), incluindo “flatten” dos Value Objects opcionais.
    public static UserProfileEntity toEntity(EcoUserProfile d) {
        Objects.requireNonNull(d, "domain must not be null");
        Objects.requireNonNull(d.getId(), "domain.id must not be null");

        var e = new UserProfileEntity();
        e.setId(d.getId().value());
        e.setExternalAuthId(fromExternalAuthIdOrNull(d.getExternalAuthId()));
        e.setFullName(d.getFullName());
        e.setEmail(fromEmailOrNull(d.getEmail()));
        e.setPhone(fromPhoneOrNull(d.getPhone()));
        e.setStatus(d.getStatus());
        e.setEmailVerified(d.isEmailVerified());
        e.setLocale(d.getLocale());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    // Converte uma String opcional em ExternalAuthId (VO) ou retorna null quando ausente/vazia.
    private static ExternalAuthId toExternalAuthIdOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return ExternalAuthId.of(raw.trim());
    }

    // Converte uma String opcional em EmailAddress (VO) ou retorna null quando ausente/vazia.
    private static EmailAddress toEmailOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return EmailAddress.of(raw.trim());
    }

    // Converte uma String opcional em PhoneNumber (VO) ou retorna null quando ausente/vazia.
    private static PhoneNumber toPhoneOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return PhoneNumber.of(raw.trim());
    }

    // Converte ExternalAuthId (VO) opcional para String (persistência) ou retorna null.
    private static String fromExternalAuthIdOrNull(ExternalAuthId id) {
        return id == null ? null : id.value();
    }

    // Converte EmailAddress (VO) opcional para String (persistência) ou retorna null.
    private static String fromEmailOrNull(EmailAddress email) {
        return email == null ? null : email.value();
    }

    // Converte PhoneNumber (VO) opcional para String (persistência) ou retorna null.
    private static String fromPhoneOrNull(PhoneNumber phone) {
        return phone == null ? null : phone.value();
    }

}
