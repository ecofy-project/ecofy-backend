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
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class UserProfileService implements
        CreateUserProfileUseCase,
        UpdateUserProfileUseCase,
        GetUserProfileUseCase {

    private final SaveUserProfilePort savePort;
    private final LoadUserProfilePort loadPort;
    private final PublishUserEventPort publishUserEventPort;
    private final IdempotencyPort idempotencyPort;
    private final UsersProperties props;

    @Override
    public UserProfileResult create(CreateUserProfileCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        validateCreate(command);

        String op = "users.createProfile";
        String reqHash = sha256(command.userId() + "|" + command.externalAuthId() + "|" + command.email() + "|" + command.phone());

        boolean first = idempotencyPort.registerOnce(op, command.idempotencyKey(), reqHash, props.idempotency().ttl());
        if (!first) {
            throw new IdempotencyViolationException("Idempotency key already used for operation=" + op);
        }

        var existing = loadPort.findById(command.userId());
        if (existing.isPresent()) {
            // idempotência tratou, mas retorno coerente
            return toResult(existing.get());
        }

        EcoUserProfile profile = EcoUserProfile.builder()
                .id(UserId.of(command.userId()))
                .externalAuthId(ExternalAuthId.of(command.externalAuthId()))
                .fullName(command.fullName())
                .email(command.email() != null ? EmailAddress.of(command.email()) : null)
                .phone(command.phone() != null ? PhoneNumber.of(command.phone()) : null)
                .status(UserStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        var saved = savePort.save(profile);
        publishUserEventPort.publishUserProfileCreated(saved.getId().value().toString(), toResult(saved));
        return toResult(saved);
    }

    @Override
    public UserProfileResult update(UpdateUserProfileCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String op = "users.updateProfile";
        String reqHash = sha256(command.userId() + "|" + command.fullName() + "|" + command.email() + "|" + command.phone() + "|" + command.status());

        boolean first = idempotencyPort.registerOnce(op, command.idempotencyKey(), reqHash, props.idempotency().ttl());
        if (!first) {
            throw new IdempotencyViolationException("Idempotency key already used for operation=" + op);
        }

        EcoUserProfile current = loadPort.findById(command.userId())
                .orElseThrow(() -> new UserProfileNotFoundException(command.userId()));

        UserStatus status = current.getStatus();
        if (command.status() != null && !command.status().isBlank()) {
            try {
                status = UserStatus.valueOf(command.status());
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException("Invalid status: " + command.status());
            }
        }

        var updated = current.toBuilder()
                .fullName(command.fullName() != null ? command.fullName() : current.getFullName())
                .email(command.email() != null ? EmailAddress.of(command.email()) : current.getEmail())
                .phone(command.phone() != null ? PhoneNumber.of(command.phone()) : current.getPhone())
                .status(status)
                .updatedAt(Instant.now())
                .build();

        var saved = savePort.save(updated);
        publishUserEventPort.publishUserProfileUpdated(saved.getId().value().toString(), toResult(saved));
        return toResult(saved);
    }

    @Override
    public UserProfileResult getById(UUID userId) {
        var profile = loadPort.findById(userId).orElseThrow(() -> new UserProfileNotFoundException(userId));
        return toResult(profile);
    }

    private static void validateCreate(CreateUserProfileCommand c) {
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.externalAuthId() == null || c.externalAuthId().isBlank()) throw new BusinessValidationException("externalAuthId is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank()) throw new BusinessValidationException("idempotencyKey is required");
    }

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