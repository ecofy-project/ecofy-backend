package br.com.ecofy.ms_categorization.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@Slf4j
public class KafkaConfig {

    /**
     * Configura o error handler com backoff exponencial para retries no @KafkaListener.
     *
     * Observação:
     * - Mantém o comportamento de retry.
     * - Evita swallow silencioso: loga erro ao “esgotar” tentativas (recoverer).
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        var backoff = new ExponentialBackOff(500L, 2.0);
        backoff.setMaxInterval(10_000L);

        return new DefaultErrorHandler((rec, ex) -> {
            // Recovery após retries: loga e "desiste" (sem DLT).
            // Se você quiser DLT, aqui é o lugar para publicar em outro tópico.
            log.error(
                    "[KafkaConfig] - [kafkaErrorHandler] -> exhausted retries topic={} partition={} offset={} key={} err={}",
                    rec.topic(), rec.partition(), rec.offset(), rec.key(), ex.toString(), ex
            );
        }, backoff);
    }

    /**
     * Cria a ContainerFactory do @KafkaListener aplicando PRIMEIRO as properties do Spring Boot (YAML),
     * e só depois adiciona o DefaultErrorHandler customizado.
     *
     * Isso garante que `spring.kafka.listener.*` (ex.: ack-mode) não seja ignorado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();

        // Aplica as configs do YAML (ack-mode, concurrency, etc) + consumerFactory autoconfigurado.
        configurer.configure(factory, consumerFactory);

        // Customização específica do serviço.
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }

    // Cria (via AdminClient) o tópico de requests consumido do ms-ingestion.
    // Fonte ÚNICA de tópicos: CategorizationProperties (prefixo ecofy.categorization.topics.*),
    // o mesmo usado pelo consumer e pelo adapter de publicação — elimina a divergência anterior
    // (KafkaConfig lia ecofy.categorization.kafka.topics, que o application.yml não definia).
    @Bean
    public NewTopic categorizationRequestTopic(CategorizationProperties props) {
        return TopicBuilder.name(props.getTopics().getCategorizationRequest())
                .partitions(6)
                .replicas(1)
                .build();
    }

    // Cria (via AdminClient) o tópico de transação categorizada publicado para o ms-budgeting.
    @Bean
    public NewTopic transactionCategorizedTopic(CategorizationProperties props) {
        return TopicBuilder.name(props.getTopics().getTransactionCategorized())
                .partitions(6)
                .replicas(1)
                .build();
    }

    // Cria (via AdminClient) o tópico de categorização aplicada (auditoria).
    @Bean
    public NewTopic categorizationAppliedTopic(CategorizationProperties props) {
        return TopicBuilder.name(props.getTopics().getCategorizationApplied())
                .partitions(6)
                .replicas(1)
                .build();
    }
}
