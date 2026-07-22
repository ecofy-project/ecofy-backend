package br.com.ecofy.ms_budgeting.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Habilita o agendamento das rotinas do serviço, permitindo desligá-lo por propriedade em testes.
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "ecofy.budgeting.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig { }
