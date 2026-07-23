package br.com.ecofy.ms_insights.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

// Centraliza as configurações externas do serviço, com defaults programáticos para os grupos opcionais.
@Validated
@ConfigurationProperties(prefix = "ecofy.insights")
public record InsightsProperties (

        @Valid Topics topics,
        @Valid Idempotency idempotency,
        @Valid Engine engine,
        @Valid Money money,
        @Valid Pagination pagination,
        @Valid Cache cache,
        @Valid Kafka kafka,
        @Valid Rebuild rebuild,
        @Valid Outbox outbox
) {

    // Agrupa e valida os tópicos Kafka usados pelo ms-insights (consumo e publicação).
    public record Topics(
            @NotBlank String categorizedTransactionTopic,
            @NotBlank String budgetAlertTopic,
            @NotBlank String insightCreatedTopic,
            String reportReadyTopic
    ) { }

    // Centraliza a configuração de idempotência do ms-insights, validando o TTL em segundos.
    public record Idempotency(
            @Min(1) @Max(604800) // 1s .. 7d
            int ttlSeconds
    ) { }

    // Motor de geração de insights (limites, score mínimo e flag de publicação de relatórios).
    public record Engine(
            @Min(10) @Max(20000)
            int maxTransactionsToAnalyze,

            @Min(0) @Max(100)
            int minScoreToPublish,

            boolean publishReports
    ) { }

    // Expõe em configuração externa a unidade monetária oficial usada pelo domínio.
    public record Money(
            int scale,
            String roundingMode,
            String defaultCurrency
    ) { }

    // Paginação das listagens (ECO-10).
    public record Pagination(
            @Min(1) int defaultSize,
            @Min(1) int maxSize
    ) { }

    // Cache seletivo (ECO-28). TTLs por recurso.
    public record Cache(
            boolean enabled,
            @NotNull Duration dashboardTtl,
            @NotNull Duration metricsTtl
    ) { }

    // Consumo e concorrência Kafka (ECO-29).
    public record Kafka(
            @Min(1) @Max(64) int concurrency,
            @NotBlank String partitionKeyField
    ) { }

    // Rebuild de insights (ECO-13).
    public record Rebuild(
            @Min(1) int batchSize,
            @Min(1) int maxPeriodDays
    ) { }

    // Transactional Outbox (ECO-03).
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
    ) { }
}
