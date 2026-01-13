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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class UserPreferenceService implements UpdateUserPreferencesUseCase, GetUserPreferencesUseCase {

    private static final String OPERATION = "users.updatePreferences";

    private final SaveUserPreferencePort savePort;
    private final LoadUserPreferencesPort loadPort;
    private final IdempotencyPort idempotencyPort;
    private final UsersProperties.Idempotency idempotencyProps;

    public UserPreferenceService(SaveUserPreferencePort savePort,
                                 LoadUserPreferencesPort loadPort,
                                 IdempotencyPort idempotencyPort,
                                 UsersProperties props) {
        this.savePort = Objects.requireNonNull(savePort, "savePort must not be null");
        this.loadPort = Objects.requireNonNull(loadPort, "loadPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        Objects.requireNonNull(props, "props must not be null");
        this.idempotencyProps = Objects.requireNonNull(props.idempotency(), "props.idempotency must not be null");
    }

    @Override
    public UserPreferencesResult update(UpdatePreferencesCommand command) {
        validate(command);

        UUID userId = command.userId();
        Map<PreferenceKey, String> normalizedPrefs = normalizePreferences(command.preferences());

        String reqHash = sha256("%s|%s".formatted(userId, stablePrefsHashInput(normalizedPrefs)));

        boolean first = idempotencyPort.registerOnce(
                OPERATION,
                command.idempotencyKey(),
                reqHash,
                idempotencyProps.ttl()
        );

        if (!first) {
            log.info(
                    "[UserPreferenceService] - [update] -> idempotency violation op={} userId={} prefKeys={}",
                    OPERATION, userId, normalizedPrefs.keySet()
            );
            throw new IdempotencyViolationException("Idempotency key already used for operation=" + OPERATION);
        }

        Instant now = Instant.now();
        List<UserPreference> toUpsert = new ArrayList<>(normalizedPrefs.size());
        UserId domainUserId = UserId.of(userId);

        for (var e : normalizedPrefs.entrySet()) {
            toUpsert.add(UserPreference.builder()
                    .id(UUID.randomUUID()) // adapter faz upsert por (userId+key)
                    .userId(domainUserId)
                    .key(e.getKey())
                    .value(blankToNull(e.getValue()))
                    .updatedAt(now)
                    .build());
        }

        var saved = savePort.upsertAll(toUpsert);

        log.info(
                "[UserPreferenceService] - [update] -> userId={} updatedKeys={} totalUpserted={}",
                userId, normalizedPrefs.keySet(), saved.size()
        );

        return getByUserId(userId);
    }

    @Override
    public UserPreferencesResult getByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        var list = loadPort.findByUserId(userId);

        // Mantém contrato atual: Map<String,String> (key como name()).
        // Ordenação estável por enum name (útil para diffs e testes).
        Map<String, String> out = new LinkedHashMap<>();
        list.stream()
                .sorted(Comparator.comparing(p -> p.getKey().name()))
                .forEach(p -> out.put(p.getKey().name(), p.getValue()));

        log.debug("[UserPreferenceService] - [getByUserId] -> userId={} size={}", userId, out.size());
        return new UserPreferencesResult(userId, out);
    }

    private static void validate(UpdatePreferencesCommand c) {
        if (c == null) throw new BusinessValidationException("command must not be null");
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.preferences() == null || c.preferences().isEmpty())
            throw new BusinessValidationException("preferences is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank())
            throw new BusinessValidationException("idempotencyKey is required");
    }

    private static Map<PreferenceKey, String> normalizePreferences(Map<PreferenceKey, String> prefs) {
        // Remove chaves nulas e normaliza valores (trim). Mantém ordem por enum natural (EnumMap).
        EnumMap<PreferenceKey, String> out = new EnumMap<>(PreferenceKey.class);
        for (var e : prefs.entrySet()) {
            if (e.getKey() == null) continue;
            out.put(e.getKey(), e.getValue() == null ? null : e.getValue().trim());
        }
        if (out.isEmpty()) throw new BusinessValidationException("preferences must contain at least one valid key");
        return out;
    }

    private static String stablePrefsHashInput(Map<PreferenceKey, String> prefs) {
        // String estável para hash: KEY=VALUE;KEY=VALUE (ordenado por enum)
        // Evita depender de toString() de Map (não determinístico em HashMap).
        StringBuilder sb = new StringBuilder();
        prefs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .forEach(e -> {
                    sb.append(e.getKey().name()).append('=').append(blankToEmpty(e.getValue())).append(';');
                });
        return sb.toString();
    }

    private static String blankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String blankToEmpty(String v) {
        if (v == null) return "";
        String t = v.trim();
        return t.isEmpty() ? "" : t;
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
