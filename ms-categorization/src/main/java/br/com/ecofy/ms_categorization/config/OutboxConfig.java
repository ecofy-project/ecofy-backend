package br.com.ecofy.ms_categorization.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

// Configura os produtores Kafka e ativa o processamento agendado da outbox.
@Configuration
@EnableScheduling
public class OutboxConfig {

    // Configura o produtor principal com as propriedades gerais do serviço.
    @Bean
    @Primary
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // Configura o produtor textual da outbox com tempo de bloqueio limitado.
    @Bean
    public ProducerFactory<String, String> outboxProducerFactory(KafkaProperties kafkaProperties,
                                                                 CategorizationProperties props) {
        Map<String, Object> config = kafkaProperties.buildProducerProperties();
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, props.getOutbox().getPublishTimeout().toMillis());

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate(
            ProducerFactory<String, String> outboxProducerFactory
    ) {
        return new KafkaTemplate<>(outboxProducerFactory);
    }
}
