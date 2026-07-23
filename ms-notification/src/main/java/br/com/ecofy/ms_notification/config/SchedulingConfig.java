package br.com.ecofy.ms_notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Habilita o agendamento do publisher da Outbox, permitindo desligá-lo por propriedade em testes.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "ecofy.notification.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
