package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
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

@Slf4j
@Service
public class AuthUserSyncService implements UpsertUserFromAuthUseCase {

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

    /**
     * Upsert genérico a partir do Auth (porta de entrada de caso de uso).
     * - Resolve fullName com fallback (fullName -> firstName + lastName)
     * - Resolve status com fallback seguro (default ACTIVE)
     * - Resolve locale (default pt-BR)
     * - Resolve emailVerified (default false)
     * - Procura por externalAuthId e atualiza/cria
     */
    @Override
    @Transactional
    public UserProfileResult upsert(Command c) {

        Instant now = Instant.now();

        ExternalAuthId externalAuthId = safeExternalAuthId(c.authUserId());
        EmailAddress email = safeEmail(c.email());

        String fullName = resolveFullName(c);
        UserStatus status = resolveStatus(c.status());
        boolean emailVerified = Boolean.TRUE.equals(c.emailVerified());
        String locale = resolveLocale(c.locale());

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
            // Em um fluxo real de sync com Auth, externalAuthId costuma ser obrigatório.
            // Se você preferir, lance uma exception de domínio aqui.
            log.warn("[AuthUserSyncService] - [upsert] -> externalAuthId missing. email={}", c.email());
        }

        Optional<EcoUserProfile> existingOpt = (externalAuthId != null)
                ? loadUserProfilePort.findByExternalAuthId(externalAuthId)
                : Optional.empty();

        EcoUserProfile profile;

        if (existingOpt.isPresent()) {
            EcoUserProfile cur = existingOpt.get();

            // Atualiza com dados do Auth (sincronização “fonte de verdade”)
            EcoUserProfile updated = applySyncedAuthData(cur, externalAuthId, email, fullName, status, emailVerified, locale, now);

            EcoUserProfile saved = saveUserProfilePort.save(updated);

            log.info(
                    "[AuthUserSyncService] - [upsert] -> synced existing profile userId={} status={}",
                    saved.getId() != null ? saved.getId().value() : null,
                    saved.getStatus()
            );

            return UserProfileResult.from(saved);
        }

        // Não existe perfil para esse externalAuthId -> cria um novo localmente
        // Observação: como o Command não tem userId interno (UUID), geramos um novo.
        // Se no seu sistema o id do ms-users deve ser o mesmo UUID do Auth, ajuste aqui.
        EcoUserProfile created = EcoUserProfile.builder()
                .id(UserId.of(UUID.randomUUID()))
                .externalAuthId(externalAuthId)
                .email(email)
                .fullName(fullName)
                .status(status != null ? status : UserStatus.ACTIVE)
                // Caso seu domínio tenha esses campos. Se não tiver, remova/ajuste.
                .emailVerified(emailVerified)
                .locale(locale)
                .createdAt(now)
                .updatedAt(now)
                .build();

        EcoUserProfile saved = saveUserProfilePort.save(created);

        log.info(
                "[AuthUserSyncService] - [upsert] -> created profile from auth sync userId={} status={}",
                saved.getId() != null ? saved.getId().value() : null,
                saved.getStatus()
        );

        return UserProfileResult.from(saved);
    }

    // --------------------------------------------------------------------------------------------
    // LEGACY / EVENT HANDLER (mantido) - agora com fallback para externalAuthId e reuso de helpers
    // --------------------------------------------------------------------------------------------

    /**
     * Sincroniza o EcoUserProfile a partir do evento "AuthUserCreated", criando um novo perfil
     * ou atualizando o existente (merge) e persistindo via ports.
     *
     * Regra:
     * - Tenta achar por userId (evento antigo)
     * - Se não achar e tiver externalAuthId, tenta achar por externalAuthId (nova lógica)
     * - Se não achar, cria com userId do evento
     */
    @Transactional
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

        // Defaults coerentes para evento antigo (não vinha com status/emailVerified/locale)
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

        // 1) Procura por ID interno (fluxo antigo)
        Optional<EcoUserProfile> existingByIdOpt = loadUserProfilePort.findById(userId);
        if (existingByIdOpt.isPresent()) {
            EcoUserProfile cur = existingByIdOpt.get();

            // Merge compatível: sincroniza dados do Auth e preserva o que não veio no evento
            EcoUserProfile updated = mergeEventIntoExisting(cur, externalAuthId, fullName, email, phone, status, emailVerified, locale, now);

            EcoUserProfile saved = saveUserProfilePort.save(updated);

            log.info(
                    "[AuthUserSyncService] - [onAuthUserCreated] -> synced existing profile userId={} status={}",
                    saved.getId().value(),
                    saved.getStatus()
            );
            return;
        }

        // 2) Se não achou por ID e tiver externalAuthId, tenta achar por externalAuthId (nova lógica)
        Optional<EcoUserProfile> existingByExternalOpt = (externalAuthId != null)
                ? loadUserProfilePort.findByExternalAuthId(externalAuthId)
                : Optional.empty();

        if (existingByExternalOpt.isPresent()) {
            EcoUserProfile cur = existingByExternalOpt.get();

            // Aqui, como o evento trouxe um userId interno, você pode decidir:
            // - manter o id atual do perfil encontrado (recomendado), ou
            // - migrar/alinhar ids (mais delicado). Mantemos o id atual por segurança.
            EcoUserProfile updated = mergeEventIntoExisting(cur, externalAuthId, fullName, email, phone, status, emailVerified, locale, now);

            EcoUserProfile saved = saveUserProfilePort.save(updated);

            log.info(
                    "[AuthUserSyncService] - [onAuthUserCreated] -> synced existing profile by externalAuthId userId={} status={}",
                    saved.getId() != null ? saved.getId().value() : null,
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
                // Caso seu domínio tenha esses campos. Se não tiver, remova/ajuste.
                .emailVerified(false)
                .locale("pt-BR")
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

    // --------------------------------------------------------------------------------------------
    // MERGE / APPLY HELPERS
    // --------------------------------------------------------------------------------------------

    // Aplica dados vindos do Auth como “fonte de verdade” para o perfil existente.
    private static EcoUserProfile applySyncedAuthData(EcoUserProfile cur,
                                                      ExternalAuthId externalAuthId,
                                                      EmailAddress email,
                                                      String fullName,
                                                      UserStatus status,
                                                      boolean emailVerified,
                                                      String locale,
                                                      Instant now) {

        // Se o Auth não mandar fullName (ou mandar vazio), preserva o que já existe.
        String effectiveFullName = (fullName != null) ? fullName : cur.getFullName();

        // Em sync com Auth, normalmente você quer atualizar email sempre que vier.
        EmailAddress effectiveEmail = (email != null) ? email : cur.getEmail();

        // Status vindo do Auth, se inválido já chega “resolvido” como ACTIVE. Se quiser preservar, ajuste aqui.
        UserStatus effectiveStatus = (status != null) ? status : (cur.getStatus() != null ? cur.getStatus() : UserStatus.ACTIVE);

        return cur.toBuilder()
                .externalAuthId(externalAuthId != null ? externalAuthId : cur.getExternalAuthId())
                .email(effectiveEmail)
                .fullName(effectiveFullName)
                .status(effectiveStatus)
                // Caso seu domínio tenha esses campos. Se não tiver, remova/ajuste.
                .emailVerified(emailVerified)
                .locale(locale)
                .updatedAt(now)
                .build();
    }

    // Faz merge de dados do evento no perfil existente, preenchendo somente o que vier no evento e atualizando updatedAt.
    private static EcoUserProfile mergeEventIntoExisting(EcoUserProfile cur,
                                                         ExternalAuthId externalAuthId,
                                                         String fullName,
                                                         EmailAddress email,
                                                         PhoneNumber phone,
                                                         UserStatus defaultStatus,
                                                         boolean emailVerified,
                                                         String locale,
                                                         Instant now) {

        UserStatus status = cur.getStatus() != null ? cur.getStatus() : defaultStatus;

        return cur.toBuilder()
                .externalAuthId(externalAuthId != null ? externalAuthId : cur.getExternalAuthId())
                .fullName(fullName != null ? fullName : cur.getFullName())
                .email(email != null ? email : cur.getEmail())
                .phone(phone != null ? phone : cur.getPhone())
                .status(status)
                // Caso seu domínio tenha esses campos. Se não tiver, remova/ajuste.
                .emailVerified(emailVerified)
                .locale(locale)
                .updatedAt(now)
                .build();
    }

    // --------------------------------------------------------------------------------------------
    // RESOLVERS (do snippet novo) + SAFETY (do snippet antigo)
    // --------------------------------------------------------------------------------------------

    private String resolveFullName(Command c) {
        if (c.fullName() != null && !c.fullName().isBlank()) return c.fullName();
        String fn = c.firstName() != null ? c.firstName().trim() : "";
        String ln = c.lastName() != null ? c.lastName().trim() : "";
        String merged = (fn + " " + ln).trim();
        return merged.isBlank() ? null : merged;
    }

    private UserStatus resolveStatus(String raw) {
        if (raw == null || raw.isBlank()) return UserStatus.ACTIVE;
        try {
            return UserStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return UserStatus.ACTIVE;
        }
    }

    private String resolveLocale(String locale) {
        return (locale == null || locale.isBlank()) ? "pt-BR" : locale;
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
