package br.com.ecofy.ms_budgeting.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

// Centraliza e valida as configurações externas do serviço.
@Validated
@ConfigurationProperties(prefix = "ecofy.budgeting")
public record BudgetingProperties(
        @Valid @NotNull Topics topics,
        @Valid @NotNull Idempotency idempotency,
        @Valid @NotNull Alerts alerts,
        Schedulers schedulers,
        Scheduling scheduling,
        @Valid Money money,
        @Valid Pagination pagination,
        @Valid Security security,
        @Valid Kafka kafka,
        @Valid Outbox outbox
) {

    @ConstructorBinding
    public BudgetingProperties {
    }

    public BudgetingProperties(
            Topics topics,
            Idempotency idempotency,
            Alerts alerts,
            Schedulers schedulers,
            Scheduling scheduling
    ) {
        this(
                topics,
                idempotency,
                alerts,
                schedulers,
                scheduling,
                null,
                null,
                null,
                null,
                null
        );
    }

    // Agrupa os tópicos Kafka utilizados pelo serviço.
    public record Topics(
            @NotBlank String categorizedTransaction,
            @NotBlank String budgetAlert
    ) {
    }

    // Define o tempo de validade das chaves de idempotência.
    public record Idempotency(
            @NotNull Duration ttl
    ) {
    }

    // Centraliza os limites e critérios de publicação dos alertas.
    public record Alerts(
            BigDecimal warningThresholdPct,
            BigDecimal criticalThresholdPct,
            boolean publishOnEveryUpdate
    ) {
    }

    // Controla a ativação das tarefas agendadas do serviço.
    public record Schedulers(
            boolean recalculationEnabled,
            boolean cleanupEnabled
    ) {
    }

    // Configura a periodicidade e a retenção das tarefas agendadas.
    public record Scheduling(
            Integer cleanupRetentionDays,
            String cleanupCron,
            String recalculateCron
    ) {
    }

    // Define o padrão monetário utilizado pelo domínio.
    public record Money(
            int scale,
            String roundingMode,
            String defaultCurrency
    ) {
    }

    // Configura os limites de paginação das consultas.
    public record Pagination(
            @Min(1) int defaultSize,
            @Min(1) int maxSize
    ) {
    }

    // Define a claim utilizada para identificar o proprietário do recurso.
    public record Security(
            @NotBlank String ownerClaim
    ) {
    }

    // Configura o consumo, a validação e o particionamento dos eventos.
    public record Kafka(
            @NotBlank String consumerGroup,
            @NotNull List<Integer> supportedEventVersions,
            @NotBlank String partitionKeyField
    ) {
    }

    // Configura o processamento transacional dos eventos da outbox.
    public record Outbox(
            @Min(1) int batchSize,
            @NotNull Duration pollInterval,
            @Min(1) int maxAttempts,
            @NotNull Duration initialBackoff,
            @Positive double backoffMultiplier,
            @NotNull Duration maxBackoff,
            @NotNull Duration processingTimeout,
            @NotNull Duration publishedRetention,
            @Min(1) int cleanupBatchSize,
            @NotNull Duration publishTimeout
    ) {
    }
}
