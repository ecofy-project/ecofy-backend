package br.com.ecofy.ms_insights.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ecofy.insights")
public record InsightsProperties (

        @Valid Topics topics,
        @Valid Idempotency idempotency,
        @Valid Engine engine
) {

    // Agrupa e valida os tópicos Kafka usados pelo ms-insights (consumo e publicação), garantindo que os obrigatórios não estejam em branco.
    public record Topics(
            @NotBlank String categorizedTransactionTopic,
            @NotBlank String budgetAlertTopic,
            @NotBlank String insightCreatedTopic,
            // Define um tópico opcional para relatórios prontos, permitindo null/vazio quando a publicação de reports não é utilizada.
            String reportReadyTopic
    ) { }

    // Centraliza a configuração de idempotência do ms-insights, validando o TTL em segundos dentro de um intervalo seguro.
    public record Idempotency(
            @Min(1) @Max(604800) // 1s .. 7d (ajuste se quiser)
            int ttlSeconds
    ) { }

    // Agrupa configurações do motor de geração de insights (limites, score mínimo e flag de publicação de relatórios) com validação de faixa.
    public record Engine(
            @Min(10) @Max(20000)
            int maxTransactionsToAnalyze,

            @Min(0) @Max(100)
            int minScoreToPublish,

            // Controla se o serviço deve publicar relatórios além de insights, habilitando/desabilitando comportamento por configuração.
            boolean publishReports
    ) { }

}
