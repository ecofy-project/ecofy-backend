package br.com.ecofy.ms_users.adapters.out.persistence.mapper;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.UserProfileEntity;
import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileMapperTest {

    private final UserProfileMapper mapper = new UserProfileMapper();

    @Test
    void roundTrip_domainToEntityToDomain_shouldPreserveEmailVerifiedAndLocale() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        EcoUserProfile original = EcoUserProfile.builder()
                .id(UserId.of(id))
                .externalAuthId(ExternalAuthId.of("auth-777"))
                .email(EmailAddress.of("round@ecofy.com"))
                .fullName("Round Trip")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .locale("en-US")
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserProfileEntity entity = UserProfileMapper.toEntity(original);

        // Entidade carrega os campos (colunas novas).
        assertTrue(entity.isEmailVerified());
        assertEquals("en-US", entity.getLocale());
        assertEquals("auth-777", entity.getExternalAuthId());

        EcoUserProfile back = mapper.toDomain(entity);

        // Round-trip não perde dados vindos do ms-auth.
        assertEquals(id, back.getId().value());
        assertEquals("auth-777", back.getExternalAuthId().value());
        assertTrue(back.isEmailVerified());
        assertEquals("en-US", back.getLocale());
        assertEquals(UserStatus.ACTIVE, back.getStatus());
    }

    @Test
    void toDomain_shouldMapDefaults_whenEmailVerifiedFalseAndLocaleNull() {
        var e = new UserProfileEntity();
        e.setId(UUID.randomUUID());
        e.setExternalAuthId("auth-1");
        e.setStatus(UserStatus.PENDING);
        e.setEmailVerified(false);
        e.setLocale(null);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());

        EcoUserProfile d = mapper.toDomain(e);

        assertFalse(d.isEmailVerified());
        assertNull(d.getLocale());
    }
}
