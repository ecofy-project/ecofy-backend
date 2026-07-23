package br.com.ecofy.ms_categorization.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

// Centraliza as configurações do serviço, validadas no boot para falhar cedo em valor inválido.
@Configuration
@ConfigurationProperties(prefix = "ecofy.categorization")
@Validated
@Getter
@Setter
@ToString
public class CategorizationProperties {

    @Valid
    @NotNull
    private Topics topics = new Topics();

    @Valid
    @NotNull
    private Idempotency idempotency = new Idempotency();

    @Valid
    @NotNull
    private RuleEngine ruleEngine = new RuleEngine();

    @Valid
    @NotNull
    private Kafka kafka = new Kafka();

    @Valid
    @NotNull
    private Outbox outbox = new Outbox();

    // Valida invariantes entre campos que o Bean Validation não expressa.
    @PostConstruct
    void validateInvariants() {
        if (outbox.getInitialBackoff().compareTo(outbox.getMaxBackoff()) > 0) {
            throw new IllegalStateException(
                    "ecofy.categorization.outbox.initial-backoff (" + outbox.getInitialBackoff()
                            + ") must not exceed max-backoff (" + outbox.getMaxBackoff() + ")");
        }
        if (kafka.getRetry().getInitialInterval().compareTo(kafka.getRetry().getMaxInterval()) > 0) {
            throw new IllegalStateException(
                    "ecofy.categorization.kafka.retry.initial-interval must not exceed max-interval");
        }
        if (!kafka.getSupportedEventVersions().contains(1)) {
            throw new IllegalStateException(
                    "ecofy.categorization.kafka.supported-event-versions must include the current version 1");
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Topics {
        // Define os tópicos Kafka usados pelo ms-categorization para consumir e publicar eventos.
        @NotBlank
        private String categorizationRequest = "eco.categorization.request";

        @NotBlank
        private String transactionCategorized = "eco.transaction.categorized";

        @NotBlank
        private String categorizationApplied = "eco.categorization.applied";
    }

    @Getter
    @Setter
    @ToString
    public static class Idempotency {
        // Define o TTL (em segundos) para chaves de idempotência, evitando reprocessamento de mensagens/eventos.
        @Min(60)
        private long ttlSeconds = 86_400;
    }

    @Getter
    @Setter
    @ToString
    public static class RuleEngine {
        // Define os parâmetros do motor de regras (limites e estratégia) usados na categorização automática.
        @Min(1)
        private int maxRulesToEvaluate = 200;

        private boolean bestScoreWins = true;

        @Min(1)
        private int minScoreToCategorize = 1;

        private boolean createSuggestionWhenUnmatched = true;
    }

    // Agrupa a configuração de consumo Kafka: retry, DLT, concorrência e versões aceitas.
    @Getter
    @Setter
    @ToString
    public static class Kafka {

        // Identifica o consumer group nos headers da DLT, apenas para diagnóstico.
        @NotBlank
        private String consumerGroup = "ms-categorization-v2";

        // Define as threads de consumo, que não devem exceder o número de partições do tópico.
        @Min(1)
        private int concurrency = 3;

        @NotBlank
        private String dltSuffix = ".dlt";

        // Define as versões de evento aceitas; versão fora da lista é erro permanente.
        @NotEmpty
        private List<Integer> supportedEventVersions = List.of(1);

        @Valid
        @NotNull
        private Retry retry = new Retry();

        @Getter
        @Setter
        @ToString
        public static class Retry {
            // Define o total de tentativas incluindo a entrega original, nunca infinito.
            @Min(1)
            private int maxAttempts = 3;

            @NotNull
            private Duration initialInterval = Duration.ofSeconds(1);

            @DecimalMin("1.0")
            private double multiplier = 2.0;

            @NotNull
            private Duration maxInterval = Duration.ofSeconds(10);
        }
    }

    // Agrupa a configuração da Transactional Outbox.
    @Getter
    @Setter
    @ToString
    public static class Outbox {

        @Min(1)
        private int batchSize = 100;

        // Define o intervalo entre ciclos do publisher.
        @NotNull
        private Duration pollInterval = Duration.ofSeconds(1);

        // Define as tentativas de publicação antes do descarte, nunca infinito.
        @Min(1)
        private int maxAttempts = 10;

        @NotNull
        private Duration initialBackoff = Duration.ofSeconds(1);

        @DecimalMin("1.0")
        private double backoffMultiplier = 2.0;

        @NotNull
        private Duration maxBackoff = Duration.ofMinutes(5);

        // Define o tempo após o qual um registro em PROCESSING é considerado abandonado.
        @NotNull
        private Duration processingTimeout = Duration.ofMinutes(5);

        // Define por quanto tempo registros publicados são retidos antes da limpeza.
        @NotNull
        private Duration publishedRetention = Duration.ofDays(7);

        // Limita o número de linhas removidas por ciclo de limpeza.
        @Min(1)
        private int cleanupBatchSize = 500;

        // Define o timeout de confirmação do broker por evento publicado.
        @NotNull
        private Duration publishTimeout = Duration.ofSeconds(10);
    }
}
