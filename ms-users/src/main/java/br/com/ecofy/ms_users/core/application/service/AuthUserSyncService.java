package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.PhoneNumber;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import br.com.ecofy.ms_users.core.port.out.SaveUserProfilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthUserSyncService {

    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;

    // Inicializa o serviço de sincronização de usuário do Auth, injetando portas de leitura e escrita de perfil.
    public AuthUserSyncService(
            LoadUserProfilePort loadUserProfilePort,
            SaveUserProfilePort saveUserProfilePort
    ) {
        this.loadUserProfilePort = Objects.requireNonNull(loadUserProfilePort, "loadUserProfilePort must not be null");
        this.saveUserProfilePort = Objects.requireNonNull(saveUserProfilePort, "saveUserProfilePort must not be null");
    }

    // Sincroniza o EcoUserProfile a partir do evento "AuthUserCreated", criando um novo perfil ou atualizando o existente (merge) e persistindo via ports.
    public void onAuthUserCreated(UUID userIdRaw,
                                  String externalAuthIdRaw,
                                  String fullNameRaw,
                                  String emailRaw,
                                  String phoneRaw) {

        UUID userId = Objects.requireNonNull(userIdRaw, "userId must not be null");
        Instant now = Instant.now();

        ExternalAuthId externalAuthId = safeExternalAuthId(externalAuthIdRaw);
        String fullName = blankToNull(fullNameRaw);
        EmailAddress email = safeEmail(emailRaw);
        PhoneNumber phone = safePhone(phoneRaw);

        log.info(
                "[AuthUserSyncService] - [onAuthUserCreated] -> userId={} hasExternalAuthId={} hasFullName={} hasEmail={} hasPhone={}",
                userId,
                externalAuthId != null,
                fullName != null,
                email != null,
                phone != null
        );

        Optional<EcoUserProfile> existingOpt = loadUserProfilePort.findById(userId);

        if (existingOpt.isPresent()) {
            EcoUserProfile cur = existingOpt.get();
            EcoUserProfile updated = mergeIntoExisting(cur, externalAuthId, fullName, email, phone, now);

            EcoUserProfile saved = saveUserProfilePort.save(updated);

            log.info(
                    "[AuthUserSyncService] - [onAuthUserCreated] -> synced existing profile userId={} status={}",
                    saved.getId().value(),
                    saved.getStatus()
            );
            return;
        }

        if (externalAuthId == null) {
            // Em evento "user created", externalAuthId normalmente é obrigatório.
            // Se você preferir, substitua por exception de domínio.
            log.warn(
                    "[AuthUserSyncService] - [onAuthUserCreated] -> externalAuthId missing for new profile. userId={}",
                    userId
            );
        }

        EcoUserProfile created = EcoUserProfile.builder()
                .id(UserId.of(userId))
                .externalAuthId(externalAuthId) // pode ser null se evento vier incompleto
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .status(UserStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        EcoUserProfile saved = saveUserProfilePort.save(created);

        log.info(
                "[AuthUserSyncService] - [onAuthUserCreated] -> created profile from auth event userId={} status={}",
                saved.getId().value(),
                saved.getStatus()
        );
    }

    // Faz merge de dados do evento no perfil existente, preenchendo apenas campos ausentes e atualizando updatedAt.
    private static EcoUserProfile mergeIntoExisting(EcoUserProfile cur,
                                                    ExternalAuthId externalAuthId,
                                                    String fullName,
                                                    EmailAddress email,
                                                    PhoneNumber phone,
                                                    Instant now) {

        UserStatus status = cur.getStatus() != null ? cur.getStatus() : UserStatus.PENDING;

        return cur.toBuilder()
                .externalAuthId(externalAuthId != null ? externalAuthId : cur.getExternalAuthId())
                .fullName(fullName != null ? fullName : cur.getFullName())
                .email(email != null ? email : cur.getEmail())
                .phone(phone != null ? phone : cur.getPhone())
                .status(status)
                .updatedAt(now)
                .build();
    }

    // Converte externalAuthId raw em Value Object, retornando null quando ausente/em branco.
    private static ExternalAuthId safeExternalAuthId(String raw) {
        String v = blankToNull(raw);
        return v == null ? null : ExternalAuthId.of(v);
    }

    // Converte email raw em Value Object, retornando null quando ausente/em branco.
    private static EmailAddress safeEmail(String raw) {
        String v = blankToNull(raw);
        return v == null ? null : EmailAddress.of(v);
    }

    // Converte phone raw em Value Object, retornando null quando ausente/em branco.
    private static PhoneNumber safePhone(String raw) {
        String v = blankToNull(raw);
        return v == null ? null : PhoneNumber.of(v);
    }

    // Normaliza uma string opcional, retornando null quando vazia/em branco e trimando quando presente.
    private static String blankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

}
