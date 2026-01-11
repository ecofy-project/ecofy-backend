package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.PhoneNumber;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import br.com.ecofy.ms_users.core.port.out.SaveUserProfilePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserSyncService {

    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;

    public void onAuthUserCreated(UUID userId, String externalAuthId, String fullName, String email, String phone) {
        var now = Instant.now();

        var existing = loadUserProfilePort.findById(userId);
        if (existing.isPresent()) {
            var cur = existing.get();
            var updated = cur.toBuilder()
                    .externalAuthId(externalAuthId != null ? ExternalAuthId.of(externalAuthId) : cur.getExternalAuthId())
                    .fullName(fullName != null ? fullName : cur.getFullName())
                    .email(email != null ? EmailAddress.of(email) : cur.getEmail())
                    .phone(phone != null ? PhoneNumber.of(phone) : cur.getPhone())
                    .status(cur.getStatus() != null ? cur.getStatus() : UserStatus.PENDING)
                    .updatedAt(now)
                    .build();

            saveUserProfilePort.save(updated);
            log.info("[AuthUserSyncService] synced existing userId={}", userId);
            return;
        }

        EcoUserProfile profile = EcoUserProfile.builder()
                .id(UserId.of(userId))
                .externalAuthId(ExternalAuthId.of(externalAuthId))
                .fullName(fullName)
                .email(email != null ? EmailAddress.of(email) : null)
                .phone(phone != null ? PhoneNumber.of(phone) : null)
                .status(UserStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        saveUserProfilePort.save(profile);
        log.info("[AuthUserSyncService] created profile from auth event userId={}", userId);
    }
}
