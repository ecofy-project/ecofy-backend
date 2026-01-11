package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.config.UsersProperties;
import br.com.ecofy.ms_users.core.application.command.UpdatePreferencesCommand;
import br.com.ecofy.ms_users.core.application.result.UserPreferencesResult;
import br.com.ecofy.ms_users.core.domain.UserPreference;
import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import br.com.ecofy.ms_users.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_users.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.in.GetUserPreferencesUseCase;
import br.com.ecofy.ms_users.core.port.in.UpdateUserPreferencesUseCase;
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_users.core.port.out.LoadUserPreferencesPort;
import br.com.ecofy.ms_users.core.port.out.SaveUserPreferencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserPreferenceService implements UpdateUserPreferencesUseCase, GetUserPreferencesUseCase {

    private final SaveUserPreferencePort savePort;
    private final LoadUserPreferencesPort loadPort;
    private final IdempotencyPort idempotencyPort;
    private final UsersProperties props;

    @Override
    public UserPreferencesResult update(UpdatePreferencesCommand command) {
        validate(command);

        String op = "users.updatePreferences";
        String reqHash = sha256(command.userId() + "|" + command.preferences().toString());

        boolean first = idempotencyPort.registerOnce(op, command.idempotencyKey(), reqHash, props.idempotency().ttl());
        if (!first) throw new IdempotencyViolationException("Idempotency key already used for operation=" + op);

        List<UserPreference> prefs = new ArrayList<>();
        for (Map.Entry<PreferenceKey, String> e : command.preferences().entrySet()) {
            prefs.add(UserPreference.builder()
                    .id(UUID.randomUUID()) // o adapter faz upsert pela chave (userId+key), id aqui é ignorável
                    .userId(UserId.of(command.userId()))
                    .key(e.getKey())
                    .value(e.getValue())
                    .updatedAt(Instant.now())
                    .build());
        }

        savePort.upsertAll(prefs);

        return getByUserId(command.userId());
    }

    @Override
    public UserPreferencesResult getByUserId(UUID userId) {
        var list = loadPort.findByUserId(userId);

        Map<String, String> out = new LinkedHashMap<>();
        for (var p : list) {
            out.put(p.getKey().name(), p.getValue());
        }

        return new UserPreferencesResult(userId, out);
    }

    private static void validate(UpdatePreferencesCommand c) {
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.preferences() == null || c.preferences().isEmpty()) throw new BusinessValidationException("preferences is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank()) throw new BusinessValidationException("idempotencyKey is required");
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