package br.com.ecofy.ms_budgeting.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Correção Dia 6 (item #5): não havia {@code @EnableScheduling} no projeto, então os
 * beans anotados com {@code @Scheduled} (recálculo/cleanup) nunca eram disparados.
 *
 * <p>Habilitado por padrão e desligável via {@code ecofy.budgeting.scheduling.enabled=false}
 * (usado no profile de teste para não iniciar jobs no contexto de testes).</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "ecofy.budgeting.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig { }
