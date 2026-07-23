package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.PhoneNumber;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.in.UpsertUserFromAuthUseCase;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import br.com.ecofy.ms_users.core.port.out.SaveUserProfilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Sincroniza perfis locais com dados recebidos do serviço de autenticação.
@Slf4j
@Service
public class AuthUserSyncService implements UpsertUserFromAuthUseCase {

    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;

    public AuthUserSyncService(
            LoadUserProfilePort loadUserProfilePort,
            SaveUserProfilePort saveUserProfilePort
    ) {
        this.loadUserProfilePort = Objects.requireNonNull(
                loadUserProfilePort,
                "loadUserProfilePort must not be null"
        );
        this.saveUserProfilePort = Objects.requireNonNull(
                saveUserProfilePort,
                "saveUserProfilePort must not be null"
        );
    }

    // Cria ou atualiza o perfil aplicando valores seguros aos dados ausentes.
    @Override
    @Transactional
    public UserProfileResult upsert(Command command) {
        Instant now = Instant.now();

        ExternalAuthId externalAuthId =
                safeExternalAuthId(command.authUserId());
        EmailAddress email = safeEmail(command.email());

        String fullName = resolveFullName(command);
        UserStatus status = resolveStatus(command.status());
        boolean emailVerified =
                Boolean.TRUE.equals(command.emailVerified());
        String locale = resolveLocale(command.locale());

        log.info(
                "[AuthUserSyncService] - [upsert] -> hasExternalAuthId={} hasEmail={} hasFullName={} status={} emailVerified={} locale={}",
                externalAuthId != null,
                email != null,
                fullName != null,
                status,
                emailVerified,
                locale
        );

        if (externalAuthId == null) {
            log.warn(
                    "[AuthUserSyncService] - [upsert] -> externalAuthId missing. email={}",
                    command.email()
            );
        }

        Optional<EcoUserProfile> existingOpt =
                externalAuthId != null
                        ? loadUserProfilePort.findByExternalAuthId(
                        externalAuthId
                )
                        : Optional.empty();

        if (existingOpt.isPresent()) {
            EcoUserProfile current = existingOpt.get();

            EcoUserProfile updated = applySyncedAuthData(
                    current,
                    externalAuthId,
                    email,
                    fullName,
                    status,
                    emailVerified,
                    locale,
                    now
            );

            EcoUserProfile saved =
                    saveUserProfilePort.save(updated);

            log.info(
                    "[AuthUserSyncService] - [upsert] -> Perfil existente sincronizado userId={} status={}",
                    saved.getId() != null
                            ? saved.getId().value()
                            : null,
                    saved.getStatus()
            );

            return UserProfileResult.from(saved);
        }

        EcoUserProfile created = EcoUserProfile.builder()
                .id(UserId.of(UUID.randomUUID()))
                .externalAuthId(externalAuthId)
                .email(email)
                .fullName(fullName)
                .status(status != null ? status : UserStatus.ACTIVE)
                .emailVerified(emailVerified)
                .locale(locale)
                .createdAt(now)
                .updatedAt(now)
                .build();

        EcoUserProfile saved = saveUserProfilePort.save(created);

        log.info(
                "[AuthUserSyncService] - [upsert] -> Perfil criado a partir de auth sync userId={} status={}",
                saved.getId() != null
                        ? saved.getId().value()
                        : null,
                saved.getStatus()
        );

        return UserProfileResult.from(saved);
    }

    // Reconcilia eventos de cadastro pelos identificadores interno e externo.
    @Transactional
    public void onAuthUserCreated(
            UUID userIdRaw,
            String externalAuthIdRaw,
            String fullNameRaw,
            String emailRaw,
            String phoneRaw
    ) {
        UUID userId = userIdRaw;
        Instant now = Instant.now();

        ExternalAuthId externalAuthId =
                safeExternalAuthId(externalAuthIdRaw);

        if (externalAuthId == null) {
            throw new BusinessValidationException(
                    "authUserId (externalAuthId) is required to sync a user profile from auth event"
            );
        }

        String fullName = blankToNull(fullNameRaw);
        EmailAddress email = safeEmail(emailRaw);
        PhoneNumber phone = safePhone(phoneRaw);

        UserStatus status = UserStatus.PENDING;
        boolean emailVerified = false;
        String locale = "pt-BR";

        log.info(
                "[AuthUserSyncService] - [onAuthUserCreated] -> userId={} hasExternalAuthId={} hasFullName={} hasEmail={} hasPhone={}",
                userId,
                externalAuthId != null,
                fullName != null,
                email != null,
                phone != null
        );

        Optional<EcoUserProfile> existingByIdOpt =
                userId != null
                        ? loadUserProfilePort.findById(userId)
                        : Optional.empty();

        if (existingByIdOpt.isPresent()) {
            EcoUserProfile current = existingByIdOpt.get();

            EcoUserProfile updated = mergeEventIntoExisting(
                    current,
                    externalAuthId,
                    fullName,
                    email,
                    phone,
                    status,
                    emailVerified,
                    locale,
                    now
            );

            EcoUserProfile saved =
                    saveUserProfilePort.save(updated);

            log.info(
                    "[AuthUserSyncService] - [onAuthUserCreated] -> Perfil existente sincronizado userId={} status={}",
                    saved.getId().value(),
                    saved.getStatus()
            );

            return;
        }

        Optional<EcoUserProfile> existingByExternalOpt =
                loadUserProfilePort.findByExternalAuthId(externalAuthId);

        if (existingByExternalOpt.isPresent()) {
            EcoUserProfile current = existingByExternalOpt.get();

            EcoUserProfile updated = mergeEventIntoExisting(
                    current,
                    externalAuthId,
                    fullName,
                    email,
                    phone,
                    status,
                    emailVerified,
                    locale,
                    now
            );

            EcoUserProfile saved =
                    saveUserProfilePort.save(updated);

            log.info(
                    "[AuthUserSyncService] - [onAuthUserCreated] -> Perfil existente sincronizado por externalAuthId userId={} status={}",
                    saved.getId() != null
                            ? saved.getId().value()
                            : null,
                    saved.getStatus()
            );

            return;
        }

        EcoUserProfile created = EcoUserProfile.builder()
                .id(UserId.of(
                        userId != null
                                ? userId
                                : UUID.randomUUID()
                ))
                .externalAuthId(externalAuthId)
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .status(UserStatus.PENDING)
                .emailVerified(false)
                .locale("pt-BR")
                .createdAt(now)
                .updatedAt(now)
                .build();

        EcoUserProfile saved = saveUserProfilePort.save(created);

        log.info(
                "[AuthUserSyncService] - [onAuthUserCreated] -> Perfil criado a partir de auth event userId={} status={}",
                saved.getId().value(),
                saved.getStatus()
        );
    }

    // Aplica os dados sincronizados preservando valores ausentes no perfil atual.
    private static EcoUserProfile applySyncedAuthData(
            EcoUserProfile current,
            ExternalAuthId externalAuthId,
            EmailAddress email,
            String fullName,
            UserStatus status,
            boolean emailVerified,
            String locale,
            Instant now
    ) {
        String effectiveFullName = fullName != null
                ? fullName
                : current.getFullName();

        EmailAddress effectiveEmail = email != null
                ? email
                : current.getEmail();

        UserStatus effectiveStatus = status != null
                ? status
                : current.getStatus() != null
                ? current.getStatus()
                : UserStatus.ACTIVE;

        return current.toBuilder()
                .externalAuthId(
                        externalAuthId != null
                                ? externalAuthId
                                : current.getExternalAuthId()
                )
                .email(effectiveEmail)
                .fullName(effectiveFullName)
                .status(effectiveStatus)
                .emailVerified(emailVerified)
                .locale(locale)
                .updatedAt(now)
                .build();
    }

    // Mescla os dados do evento sem remover informações previamente cadastradas.
    private static EcoUserProfile mergeEventIntoExisting(
            EcoUserProfile current,
            ExternalAuthId externalAuthId,
            String fullName,
            EmailAddress email,
            PhoneNumber phone,
            UserStatus defaultStatus,
            boolean emailVerified,
            String locale,
            Instant now
    ) {
        UserStatus status = current.getStatus() != null
                ? current.getStatus()
                : defaultStatus;

        return current.toBuilder()
                .externalAuthId(
                        externalAuthId != null
                                ? externalAuthId
                                : current.getExternalAuthId()
                )
                .fullName(
                        fullName != null
                                ? fullName
                                : current.getFullName()
                )
                .email(
                        email != null
                                ? email
                                : current.getEmail()
                )
                .phone(
                        phone != null
                                ? phone
                                : current.getPhone()
                )
                .status(status)
                .emailVerified(emailVerified)
                .locale(locale)
                .updatedAt(now)
                .build();
    }

    private String resolveFullName(Command command) {
        if (command.fullName() != null
                && !command.fullName().isBlank()) {
            return command.fullName();
        }

        String firstName = command.firstName() != null
                ? command.firstName().trim()
                : "";
        String lastName = command.lastName() != null
                ? command.lastName().trim()
                : "";
        String merged = (firstName + " " + lastName).trim();

        return merged.isBlank() ? null : merged;
    }

    private UserStatus resolveStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return UserStatus.ACTIVE;
        }

        try {
            return UserStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return UserStatus.ACTIVE;
        }
    }

    private String resolveLocale(String locale) {
        return locale == null || locale.isBlank()
                ? "pt-BR"
                : locale;
    }

    // Converte o identificador externo informado em um objeto de valor opcional.
    private static ExternalAuthId safeExternalAuthId(String raw) {
        String value = blankToNull(raw);

        return value == null
                ? null
                : ExternalAuthId.of(value);
    }

    // Converte o endereço informado em um objeto de valor opcional.
    private static EmailAddress safeEmail(String raw) {
        String value = blankToNull(raw);

        return value == null
                ? null
                : EmailAddress.of(value);
    }

    // Converte o telefone informado em um objeto de valor opcional.
    private static PhoneNumber safePhone(String raw) {
        String value = blankToNull(raw);

        return value == null
                ? null
                : PhoneNumber.of(value);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
