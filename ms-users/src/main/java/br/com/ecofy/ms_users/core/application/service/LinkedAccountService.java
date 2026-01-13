package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.config.UsersProperties;
import br.com.ecofy.ms_users.core.application.command.LinkAccountCommand;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.domain.LinkedAccount;
import br.com.ecofy.ms_users.core.domain.enums.AccountProvider;
import br.com.ecofy.ms_users.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_users.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_users.core.domain.exception.UserProfileNotFoundException;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.in.LinkAccountUseCase;
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import br.com.ecofy.ms_users.core.port.out.SaveLinkedAccountPort;
import br.com.ecofy.ms_users.core.port.out.SaveUserProfilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class LinkedAccountService implements LinkAccountUseCase {

    private static final String OPERATION = "users.linkAccount";

    private final SaveLinkedAccountPort saveLinkedAccountPort;
    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;
    private final IdempotencyPort idempotencyPort;
    private final UsersProperties.Idempotency idempotencyProps;

    public LinkedAccountService(SaveLinkedAccountPort saveLinkedAccountPort,
                                LoadUserProfilePort loadUserProfilePort,
                                SaveUserProfilePort saveUserProfilePort,
                                IdempotencyPort idempotencyPort,
                                UsersProperties props) {
        this.saveLinkedAccountPort = Objects.requireNonNull(saveLinkedAccountPort, "saveLinkedAccountPort must not be null");
        this.loadUserProfilePort = Objects.requireNonNull(loadUserProfilePort, "loadUserProfilePort must not be null");
        this.saveUserProfilePort = Objects.requireNonNull(saveUserProfilePort, "saveUserProfilePort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        Objects.requireNonNull(props, "props must not be null");
        this.idempotencyProps = Objects.requireNonNull(props.idempotency(), "props.idempotency must not be null");
    }

    @Override
    public UserProfileResult linkAccount(LinkAccountCommand command) {
        validate(command);

        String requestHash = sha256("%s|%s|%s|%s".formatted(
                command.userId(),
                command.provider(),
                command.externalAccountRef(),
                command.active()
        ));

        boolean first = idempotencyPort.registerOnce(
                OPERATION,
                command.idempotencyKey(),
                requestHash,
                idempotencyProps.ttl()
        );

        if (!first) {
            log.info(
                    "[LinkedAccountService] - [linkAccount] -> idempotency violation op={} userId={} provider={} externalAccountRef={}",
                    OPERATION,
                    command.userId(),
                    command.provider(),
                    safeRef(command.externalAccountRef())
            );
            throw new IdempotencyViolationException("Idempotency key already used for operation=" + OPERATION);
        }

        var profile = loadUserProfilePort.findById(command.userId())
                .orElseThrow(() -> {
                    log.warn("[LinkedAccountService] - [linkAccount] -> profile not found userId={}", command.userId());
                    return new UserProfileNotFoundException(command.userId());
                });

        AccountProvider provider = parseProviderOrDefault(command.provider());

        Instant now = Instant.now();

        var account = LinkedAccount.builder()
                .id(UUID.randomUUID())
                .userId(UserId.of(command.userId()))
                .provider(provider)
                .externalAccountRef(command.externalAccountRef().trim())
                .active(command.active())
                .linkedAt(now)
                .build();

        var savedAccount = saveLinkedAccountPort.save(account);

        // opcional: touch no updatedAt do profile
        var updatedProfile = profile.toBuilder()
                .updatedAt(now)
                .build();

        var savedProfile = saveUserProfilePort.save(updatedProfile);

        log.info(
                "[LinkedAccountService] - [linkAccount] -> linkedAccountId={} userId={} provider={} active={} profileUpdatedAt={}",
                savedAccount.getId(),
                savedProfile.getId().value(),
                provider.name(),
                savedAccount.isActive(),
                savedProfile.getUpdatedAt()
        );

        return toResult(savedProfile);
    }

    private static void validate(LinkAccountCommand c) {
        if (c == null) throw new BusinessValidationException("command must not be null");
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.provider() == null || c.provider().isBlank()) throw new BusinessValidationException("provider is required");
        if (c.externalAccountRef() == null || c.externalAccountRef().isBlank())
            throw new BusinessValidationException("externalAccountRef is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank())
            throw new BusinessValidationException("idempotencyKey is required");
    }

    private static AccountProvider parseProviderOrDefault(String raw) {
        if (raw == null) return AccountProvider.OTHER;
        try {
            return AccountProvider.valueOf(raw.trim());
        } catch (Exception ignored) {
            return AccountProvider.OTHER;
        }
    }

    private static UserProfileResult toResult(br.com.ecofy.ms_users.core.domain.EcoUserProfile p) {
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

    private static String safeRef(String ref) {
        if (ref == null || ref.isBlank()) return "<empty>";
        String t = ref.trim();
        if (t.length() <= 6) return "***";
        return t.substring(0, 3) + "..." + t.substring(t.length() - 2);
    }

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
