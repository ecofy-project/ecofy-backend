package br.com.ecofy.ms_budgeting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "ecofy.budgeting")
public record BudgetingProperties(
        Topics topics,
        Idempotency idempotency,
        Alerts alerts,
        Schedulers schedulers,
        Scheduling scheduling
) {
    // Agrupa os nomes de tópicos Kafka consumidos/publicados pelo serviço.
    public record Topics(
            String categorizedTransaction,
            String budgetAlert
    ) {}

    // Define a configuração de TTL usada no controle de idempotência.
    public record Idempotency(
            Duration ttl
    ) {}

    // Centraliza thresholds e flags de publicação para geração de alertas de orçamento.
    public record Alerts(
            BigDecimal warningThresholdPct,
            BigDecimal criticalThresholdPct,
            boolean publishOnEveryUpdate
    ) {}

    // Habilita/desabilita schedulers internos do serviço.
    public record Schedulers(
            boolean recalculationEnabled,
            boolean cleanupEnabled
    ) {}

    // Configura cron e retenção para tarefas agendadas do serviço.
    public record Scheduling(
            Integer cleanupRetentionDays,
            String cleanupCron,
            String recalculateCron
    ) {}
}
