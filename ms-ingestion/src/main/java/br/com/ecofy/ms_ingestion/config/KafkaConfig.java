package br.com.ecofy.ms_ingestion.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {

    // Expõe as configurações de tópicos via properties (ecofy.ingestion.kafka.topics.*).
    @Bean
    @ConfigurationProperties("ecofy.ingestion.kafka.topics")
    public IngestionTopics ingestionTopics() {
        return new IngestionTopics();
    }

    /**
     * Configura a factory de consumidores Kafka para mensagens String (key/value).
     *
     * Observação:
     * - Producer/KafkaTemplate NÃO são configurados aqui; ficam 100% no application.yaml via autoconfig do Spring Boot.
     * - Isso evita conflito de configuração (setters vs properties) no JsonSerializer.
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties) {

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        log.info("[KafkaConfig] [consumerFactory] bootstrapServers={}", kafkaProperties.getBootstrapServers());

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    // Cria (via AdminClient) o tópico de eventos de transações importadas.
    @Bean
    public NewTopic ingestionTransactionImportedTopic(IngestionTopics topics) {
        return TopicBuilder.name(topics.getTransactionImported())
                .partitions(6)
                .replicas(1)
                .build();
    }

    // Cria (via AdminClient) o tópico de requests para categorização.
    @Bean
    public NewTopic ingestionCategorizationRequestTopic(IngestionTopics topics) {
        return TopicBuilder.name(topics.getCategorizationRequest())
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Getter
    @Setter
    public static class IngestionTopics {
        private String transactionImported = "eco.ingestion.transaction.imported";
        private String importJobStatusChanged = "eco.ingestion.import-job.status-changed";
        private String categorizationRequest = "eco.categorization.request";
    }
}
