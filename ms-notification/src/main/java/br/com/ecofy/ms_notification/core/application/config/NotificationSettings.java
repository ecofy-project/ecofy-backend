package br.com.ecofy.ms_notification.core.application.config;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

// Expõe as configurações de runtime ao core em tipo neutro, sem acoplamento ao Spring.
public record NotificationSettings(
        boolean idempotencyEnabled,
        int retryMaxAttempts,
        Duration retryBaseBackoff,
        double retryMultiplier,
        Duration retryMaxBackoff,
        Map<String, String> defaultChannelsByEventType
) {

    public NotificationSettings {
        Objects.requireNonNull(retryBaseBackoff, "retryBaseBackoff must not be null");
        if (retryMaxBackoff == null) retryMaxBackoff = Duration.ofMinutes(5);
        defaultChannelsByEventType = defaultChannelsByEventType == null
                ? Map.of()
                : Map.copyOf(defaultChannelsByEventType);
    }

    // Mantém compatibilidade com chamadas anteriores, aplicando um teto de backoff padrão.
    public NotificationSettings(boolean idempotencyEnabled, int retryMaxAttempts, Duration retryBaseBackoff,
                                double retryMultiplier, Map<String, String> defaultChannelsByEventType) {
        this(idempotencyEnabled, retryMaxAttempts, retryBaseBackoff, retryMultiplier,
                Duration.ofMinutes(5), defaultChannelsByEventType);
    }

    // Retorna o canal padrão configurado para um tipo de evento (ou null se não configurado).
    public String defaultChannelFor(String eventTypeName) {
        return defaultChannelsByEventType.get(eventTypeName);
    }
}
