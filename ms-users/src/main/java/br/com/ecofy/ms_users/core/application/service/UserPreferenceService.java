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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class UserPreferenceService implements UpdateUserPreferencesUseCase, GetUserPreferencesUseCase {

    private static final String OPERATION = "users.updatePreferences";

    private final SaveUserPreferencePort savePort;
    private final LoadUserPreferencesPort loadPort;
    private final IdempotencyPort idempotencyPort;
    private final UsersProperties.Idempotency idempotencyProps;

    // Inicializa o serviço de preferências do usuário, injetando portas de persistência/consulta e configurações de idempotência.
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

    // Atualiza preferências do usuário com idempotência, normaliza entradas (inclui validações mínimas), realiza upsert em lote e retorna o estado consolidado.
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

    // Recupera as preferências do usuário pelo userId e retorna um resultado estável (Map ordenado) para consumo externo.
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

    // Valida campos obrigatórios do comando de atualização de preferências e lança BusinessValidationException quando inválidos.
    private static void validate(UpdatePreferencesCommand c) {
        if (c == null) throw new BusinessValidationException("command must not be null");
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.preferences() == null || c.preferences().isEmpty())
            throw new BusinessValidationException("preferences is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank())
            throw new BusinessValidationException("idempotencyKey is required");
    }

    // Normaliza o mapa de preferências (remove chaves nulas, trim em valores), valida entradas e garante que existe pelo menos uma chave válida.
    private static Map<PreferenceKey, String> normalizePreferences(Map<PreferenceKey, String> prefs) {
        // Remove chaves nulas e normaliza valores (trim). Mantém ordem por enum natural (EnumMap).
        EnumMap<PreferenceKey, String> out = new EnumMap<>(PreferenceKey.class);

        for (var e : prefs.entrySet()) {
            PreferenceKey key = e.getKey();
            if (key == null) continue;

            String raw = e.getValue();
            String value = raw == null ? null : raw.trim();

            // Permite "limpar" preferência enviando vazio (persistirá null).
            if (value != null && value.isBlank()) value = null;

            // Validações mínimas por chave (fail-fast com mensagem clara).
            validatePreference(key, value);

            // Normalizações simples (ex.: moeda uppercase, locale padronizado, theme uppercase, channels normalizado).
            value = normalizeValue(key, value);

            out.put(key, value);
        }

        if (out.isEmpty()) throw new BusinessValidationException("preferences must contain at least one valid key");
        return out;
    }

    // Valida minimamente o valor conforme a chave. Se value for null, interpreta como "limpar preferência" (permitido).
    private static void validatePreference(PreferenceKey key, String value) {
        if (value == null) return;

        switch (key) {
            case DEFAULT_CURRENCY -> validateCurrency(value);
            case LOCALE -> validateLocale(value);
            case TIMEZONE -> validateTimezone(value);
            case DATE_FORMAT -> validateDateFormat(value);
            case THEME -> validateTheme(value);
            case NOTIFY_CHANNELS -> validateNotifyChannels(value);
        }
    }

    // Normaliza valores de preferências quando aplicável (sem alterar semântica).
    private static String normalizeValue(PreferenceKey key, String value) {
        if (value == null) return null;

        return switch (key) {
            case DEFAULT_CURRENCY -> value.toUpperCase(Locale.ROOT);
            case LOCALE -> normalizeLocale(value);
            case THEME -> value.toUpperCase(Locale.ROOT);
            case NOTIFY_CHANNELS -> normalizeNotifyChannels(value);
            case TIMEZONE, DATE_FORMAT -> value;
        };
    }

    // Validação mínima: 3 letras (ISO-like) para código de moeda.
    private static void validateCurrency(String v) {
        String s = v.trim();
        if (!s.matches("^[A-Za-z]{3}$")) {
            throw new BusinessValidationException("DEFAULT_CURRENCY must be a 3-letter code (e.g., BRL, USD, EUR)");
        }
    }

    // Validação mínima: "ll" ou "ll-CC" (ex.: pt-BR, en-US).
    private static void validateLocale(String v) {
        String s = v.trim();
        if (!s.matches("^[a-zA-Z]{2}(-[a-zA-Z]{2})?$")) {
            throw new BusinessValidationException("LOCALE must be in format ll or ll-CC (e.g., pt-BR, en-US)");
        }
    }

    // Normaliza locale para "ll" ou "ll-CC" (minúsculo/maiúsculo).
    private static String normalizeLocale(String v) {
        String s = v.trim();
        if (s.matches("^[a-zA-Z]{2}$")) {
            return s.toLowerCase(Locale.ROOT);
        }
        String[] parts = s.split("-");
        return parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT);
    }

    // Validação mínima: precisa ser um ZoneId IANA válido.
    private static void validateTimezone(String v) {
        try {
            ZoneId.of(v.trim());
        } catch (Exception ex) {
            throw new BusinessValidationException("TIMEZONE must be a valid IANA ZoneId (e.g., America/Sao_Paulo)");
        }
    }

    // Validação mínima: pattern aceito por DateTimeFormatter.
    private static void validateDateFormat(String v) {
        try {
            DateTimeFormatter.ofPattern(v.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("DATE_FORMAT must be a valid DateTimeFormatter pattern (e.g., yyyy-MM-dd)");
        }
    }

    // Validação mínima: tema deve estar dentro do conjunto permitido (case-insensitive).
    private static void validateTheme(String v) {
        String s = v.trim().toUpperCase(Locale.ROOT);
        if (!(s.equals("LIGHT") || s.equals("DARK") || s.equals("SYSTEM"))) {
            throw new BusinessValidationException("THEME must be one of: LIGHT, DARK, SYSTEM");
        }
    }

    // Validação mínima: lista CSV com valores permitidos. Também aceita "NONE".
    private static void validateNotifyChannels(String v) {
        String s = v.trim();
        if (s.isBlank()) return;

        String[] parts = s.split(",");
        for (String p : parts) {
            String token = p.trim().toUpperCase(Locale.ROOT);
            if (token.isEmpty()) continue;

            if (!(token.equals("EMAIL")
                    || token.equals("SMS")
                    || token.equals("PUSH")
                    || token.equals("WHATSAPP")
                    || token.equals("NONE"))) {
                throw new BusinessValidationException(
                        "NOTIFY_CHANNELS contains invalid value: " + token + " (allowed: EMAIL,SMS,PUSH,WHATSAPP,NONE)"
                );
            }
        }
    }

    // Normaliza canais: uppercase, remove espaços redundantes e duplicados, preservando ordem.
    private static String normalizeNotifyChannels(String v) {
        String[] parts = v.split(",");
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String p : parts) {
            String token = p.trim().toUpperCase(Locale.ROOT);
            if (!token.isEmpty()) set.add(token);
        }
        return String.join(",", set);
    }

    // Monta uma representação determinística das preferências (ordenada) para uso em hashing de idempotência.
    private static String stablePrefsHashInput(Map<PreferenceKey, String> prefs) {
        // String estável para hash: KEY=VALUE;KEY=VALUE (ordenado por enum)
        // Evita depender de toString() de Map (não determinístico em HashMap).
        StringBuilder sb = new StringBuilder();
        prefs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .forEach(e -> sb.append(e.getKey().name()).append('=').append(blankToEmpty(e.getValue())).append(';'));
        return sb.toString();
    }

    // Normaliza string opcional, retornando null quando vazia/em branco e trimando quando presente.
    private static String blankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    // Normaliza string opcional, retornando string vazia quando ausente/vazia e trimando quando presente.
    private static String blankToEmpty(String v) {
        if (v == null) return "";
        String t = v.trim();
        return t.isEmpty() ? "" : t;
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
