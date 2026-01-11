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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkedAccountService implements LinkAccountUseCase {

    private final SaveLinkedAccountPort saveLinkedAccountPort;
    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;
    private final IdempotencyPort idempotencyPort;
    private final UsersProperties props;

    @Override
    public UserProfileResult linkAccount(LinkAccountCommand command) {
        validate(command);

        String op = "users.linkAccount";
        String reqHash = sha256(command.userId() + "|" + command.provider() + "|" + command.externalAccountRef() + "|" + command.active());

        boolean first = idempotencyPort.registerOnce(op, command.idempotencyKey(), reqHash, props.idempotency().ttl());
        if (!first) throw new IdempotencyViolationException("Idempotency key already used for operation=" + op);

        var profile = loadUserProfilePort.findById(command.userId())
                .orElseThrow(() -> new UserProfileNotFoundException(command.userId()));

        AccountProvider provider;
        try { provider = AccountProvider.valueOf(command.provider()); }
        catch (Exception e) { provider = AccountProvider.OTHER; }

        var acc = LinkedAccount.builder()
                .id(UUID.randomUUID())
                .userId(UserId.of(command.userId()))
                .provider(provider)
                .externalAccountRef(command.externalAccountRef())
                .active(command.active())
                .linkedAt(Instant.now())
                .build();

        saveLinkedAccountPort.save(acc);

        // opcional: touch no updatedAt do profile
        var updated = profile.toBuilder().updatedAt(Instant.now()).build();
        var saved = saveUserProfilePort.save(updated);

        return new UserProfileResult(
                saved.getId().value(),
                saved.getExternalAuthId() != null ? saved.getExternalAuthId().value() : null,
                saved.getFullName(),
                saved.getEmail() != null ? saved.getEmail().value() : null,
                saved.getPhone() != null ? saved.getPhone().value() : null,
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    private static void validate(LinkAccountCommand c) {
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.provider() == null || c.provider().isBlank()) throw new BusinessValidationException("provider is required");
        if (c.externalAccountRef() == null || c.externalAccountRef().isBlank())
            throw new BusinessValidationException("externalAccountRef is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank())
            throw new BusinessValidationException("idempotencyKey is required");
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