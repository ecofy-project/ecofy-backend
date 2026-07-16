package br.com.ecofy.ms_notification.core.application.config;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Configurações de runtime do core, em tipo NEUTRO (sem Spring / sem @ConfigurationProperties).
 *
 * <p>Correção Dia 7 (item #5): antes os services de aplicação (core) importavam diretamente
 * {@code config.NotificationProperties} (classe Spring), quebrando a direção de dependência
 * hexagonal e reduzindo testabilidade. Agora o core depende apenas deste record; a camada de
 * config faz o mapeamento de {@code NotificationProperties} para {@code NotificationSettings}.</p>
 */
public record NotificationSettings(
        boolean idempotencyEnabled,
        int retryMaxAttempts,
        Duration retryBaseBackoff,
        double retryMultiplier,
        Map<String, String> defaultChannelsByEventType
) {

    public NotificationSettings {
        Objects.requireNonNull(retryBaseBackoff, "retryBaseBackoff must not be null");
        defaultChannelsByEventType = defaultChannelsByEventType == null
                ? Map.of()
                : Map.copyOf(defaultChannelsByEventType);
    }

    // Retorna o canal padrão configurado para um tipo de evento (ou null se não configurado).
    public String defaultChannelFor(String eventTypeName) {
        return defaultChannelsByEventType.get(eventTypeName);
    }
}
