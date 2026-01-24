package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.config.UsersProperties;
import br.com.ecofy.ms_users.core.application.command.CreateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.command.UpdateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_users.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_users.core.domain.exception.UserProfileNotFoundException;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.PhoneNumber;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.in.CreateUserProfileUseCase;
import br.com.ecofy.ms_users.core.port.in.GetUserProfileUseCase;
import br.com.ecofy.ms_users.core.port.in.UpdateUserProfileUseCase;
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import br.com.ecofy.ms_users.core.port.out.PublishUserEventPort;
import br.com.ecofy.ms_users.core.port.out.SaveUserProfilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserProfileService implements
        CreateUserProfileUseCase,
        UpdateUserProfileUseCase,
        GetUserProfileUseCase {

    private static final String OP_CREATE = "users.createProfile";
    private static final String OP_UPDATE = "users.updateProfile";

    private final SaveUserProfilePort savePort;
    private final LoadUserProfilePort loadPort;
    private final PublishUserEventPort publishUserEventPort;
    private final IdempotencyPort idempotencyPort;
    private final UsersProperties.Idempotency idempotencyProps;

    // Inicializa o serviço de perfil do usuário, injetando portas de persistência/consulta, publicação de eventos e configurações de idempotência.
    public UserProfileService(SaveUserProfilePort savePort,
                              LoadUserProfilePort loadPort,
                              PublishUserEventPort publishUserEventPort,
                              IdempotencyPort idempotencyPort,
                              UsersProperties props) {
        this.savePort = Objects.requireNonNull(savePort, "savePort must not be null");
        this.loadPort = Objects.requireNonNull(loadPort, "loadPort must not be null");
        this.publishUserEventPort = Objects.requireNonNull(publishUserEventPort, "publishUserEventPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        Objects.requireNonNull(props, "props must not be null");
        this.idempotencyProps = Objects.requireNonNull(props.idempotency(), "props.idempotency must not be null");
    }

    // Cria um novo perfil de usuário com idempotência, persistindo-o e publicando o evento de "profile created".
    @Override
    public UserProfileResult create(CreateUserProfileCommand command) {
        validateCreate(command);

        UUID userId = command.userId();
        String externalAuthId = command.externalAuthId();
        String email = blankToNull(command.email());
        String phone = blankToNull(command.phone());

        String reqHash = sha256(stableHashInput(
                userId,
                externalAuthId,
                blankToNull(command.fullName()),
                email,
                phone
        ));

        boolean first = idempotencyPort.registerOnce(
                OP_CREATE,
                command.idempotencyKey(),
                reqHash,
                idempotencyProps.ttl()
        );

        if (!first) {
            log.info("[UserProfileService] - [create] -> idempotency violation op={} userId={}", OP_CREATE, userId);
            throw new IdempotencyViolationException("Idempotency key already used for operation=" + OP_CREATE);
        }

        Optional<EcoUserProfile> existing = loadPort.findById(userId);
        if (existing.isPresent()) {
            // Idempotência já garantiu que este request não será processado 2x; retornar o estado atual é ok.
            EcoUserProfile cur = existing.get();
            log.info("[UserProfileService] - [create] -> already exists userId={} status={}", userId, cur.getStatus());
            return toResult(cur);
        }

        Instant now = Instant.now();

        EcoUserProfile profile = EcoUserProfile.builder()
                .id(UserId.of(userId))
                .externalAuthId(ExternalAuthId.of(externalAuthId))
                .fullName(blankToNull(command.fullName()))
                .email(email != null ? EmailAddress.of(email) : null)
                .phone(phone != null ? PhoneNumber.of(phone) : null)
                .status(UserStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        EcoUserProfile saved = savePort.save(profile);

        // Evento: payload já no shape de API/Result (mantendo seu contrato atual)
        publishUserEventPort.publishUserProfileCreated(saved.getId().value().toString(), toResult(saved));

        log.info(
                "[UserProfileService] - [create] -> userId={} status={} hasEmail={} hasPhone={}",
                saved.getId().value(),
                saved.getStatus(),
                saved.getEmail() != null,
                saved.getPhone() != null
        );

        return toResult(saved);
    }

    // Atualiza campos do perfil de usuário com idempotência, persistindo mudanças e publicando o evento de "profile updated".
    @Override
    public UserProfileResult update(UpdateUserProfileCommand command) {
        validateUpdate(command);

        UUID userId = command.userId();

        String reqHash = sha256(stableHashInput(
                userId,
                blankToNull(command.fullName()),
                blankToNull(command.email()),
                blankToNull(command.phone()),
                blankToNull(command.status())
        ));

        boolean first = idempotencyPort.registerOnce(
                OP_UPDATE,
                command.idempotencyKey(),
                reqHash,
                idempotencyProps.ttl()
        );

        if (!first) {
            log.info("[UserProfileService] - [update] -> idempotency violation op={} userId={}", OP_UPDATE, userId);
            throw new IdempotencyViolationException("Idempotency key already used for operation=" + OP_UPDATE);
        }

        EcoUserProfile current = loadPort.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        UserStatus newStatus = resolveStatus(command.status(), current.getStatus());

        EcoUserProfile updated = current.toBuilder()
                .fullName(firstNonBlank(command.fullName(), current.getFullName()))
                .email(command.email() != null ? EmailAddress.of(command.email()) : current.getEmail())
                .phone(command.phone() != null ? PhoneNumber.of(command.phone()) : current.getPhone())
                .status(newStatus)
                .updatedAt(Instant.now())
                .build();

        EcoUserProfile saved = savePort.save(updated);

        publishUserEventPort.publishUserProfileUpdated(saved.getId().value().toString(), toResult(saved));

        log.info(
                "[UserProfileService] - [update] -> userId={} status={} changedName={} changedEmail={} changedPhone={}",
                saved.getId().value(),
                saved.getStatus(),
                command.fullName() != null,
                command.email() != null,
                command.phone() != null
        );

        return toResult(saved);
    }

    // Recupera o perfil do usuário pelo userId e retorna o resultado, lançando UserProfileNotFoundException quando não encontrado.
    @Override
    public UserProfileResult getById(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        EcoUserProfile profile = loadPort.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId));

        log.debug("[UserProfileService] - [getById] -> userId={} status={}", userId, profile.getStatus());
        return toResult(profile);
    }

    // Valida campos obrigatórios do comando de criação de perfil e lança BusinessValidationException quando inválidos.
    private static void validateCreate(CreateUserProfileCommand c) {
        if (c == null) throw new BusinessValidationException("command must not be null");
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.externalAuthId() == null || c.externalAuthId().isBlank())
            throw new BusinessValidationException("externalAuthId is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank())
            throw new BusinessValidationException("idempotencyKey is required");
    }

    // Valida campos obrigatórios do comando de atualização e garante que pelo menos um campo mutável foi informado.
    private static void validateUpdate(UpdateUserProfileCommand c) {
        if (c == null) throw new BusinessValidationException("command must not be null");
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank())
            throw new BusinessValidationException("idempotencyKey is required");

        // Pelo menos 1 campo mutável deve ser enviado (evita no-op com idempotency burn)
        boolean hasAnyUpdate =
                (c.fullName() != null) ||
                        (c.email() != null) ||
                        (c.phone() != null) ||
                        (c.status() != null && !c.status().isBlank());

        if (!hasAnyUpdate) throw new BusinessValidationException("at least one field must be provided to update");
    }

    // Resolve e valida o status enviado (String) para UserStatus, usando um fallback quando ausente e rejeitando valores inválidos.
    private static UserStatus resolveStatus(String raw, UserStatus fallback) {
        if (raw == null || raw.isBlank()) return fallback != null ? fallback : UserStatus.PENDING;
        try {
            return UserStatus.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("Invalid status: " + raw);
        }
    }

    // Retorna a primeira string não-vazia entre preferida e fallback, útil para merge de atualizações parciais.
    private static String firstNonBlank(String preferred, String fallback) {
        String p = blankToNull(preferred);
        return p != null ? p : fallback;
    }

    // Normaliza uma string opcional, retornando null quando vazia/em branco e trimando quando presente.
    private static String blankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    // Monta uma string determinística a partir de partes ordenadas para hashing de idempotência.
    private static String stableHashInput(Object... parts) {
        // input estável p/ idempotência (evita NPE e mantém ordem)
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            sb.append(p == null ? "<null>" : p.toString()).append('|');
        }
        return sb.toString();
    }

    // Converte um EcoUserProfile (domínio) para UserProfileResult (DTO de saída/publicação).
    private static UserProfileResult toResult(EcoUserProfile p) {
        return new UserProfileResult(
                p.getId().value(),
                p.getExternalAuthId() != null ? p.getExternalAuthId().value() : null,
                p.getFullName(),
                p.getEmail() != null ? p.getEmail().value() : null,
                p.getPhone() != null ? p.getPhone().value() : null,
                p.getStatus(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    // Calcula SHA-256 de uma string e retorna o hex; em falha, retorna um placeholder.
    private static String sha256(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            return "sha256_error";
        }
    }

}
