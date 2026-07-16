package br.com.ecofy.ms_notification.config;

import br.com.ecofy.ms_notification.core.application.config.NotificationSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Correção Dia 7 (item #5): faz a ponte entre a config Spring ({@link NotificationProperties})
 * e o tipo neutro do core ({@link NotificationSettings}), mantendo Spring/config fora do core.
 */
@Configuration
public class NotificationCoreConfig {

    @Bean
    public NotificationSettings notificationSettings(NotificationProperties props) {
        var idempotency = props.getIdempotency();
        var retry = props.getRetry();
        var templates = props.getTemplates();

        return new NotificationSettings(
                idempotency.isEnabled(),
                retry.getMaxAttempts(),
                retry.getBaseBackoff(),
                retry.getMultiplier(),
                templates.getDefaultChannels()
        );
    }
}
