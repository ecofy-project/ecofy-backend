package br.com.ecofy.ms_notification.config;

import br.com.ecofy.ms_notification.core.application.config.NotificationSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Converte as propriedades Spring no tipo neutro consumido pelo core, mantendo o framework fora dele.
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
                retry.getMaxBackoff(),
                templates.getDefaultChannels()
        );
    }
}
