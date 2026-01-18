package br.com.ecofy.ms_categorization.config;

import br.com.ecofy.ms_categorization.adapters.in.kafka.dto.CategorizationRequestMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    // Cria a ConsumerFactory configurada para desserializar mensagens Kafka em CategorizationRequestMessage usando Jackson.
    @Bean
    public ConsumerFactory<String, CategorizationRequestMessage> consumerFactory(
            KafkaProperties kafkaProperties,
            JsonMapper jsonMapper
    ) {
        Map<String, Object> cfg = new HashMap<>(kafkaProperties.buildConsumerProperties());

        cfg.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cfg.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);

        var valueDeserializer = new JacksonJsonDeserializer<>(CategorizationRequestMessage.class, jsonMapper);

        valueDeserializer.ignoreTypeHeaders();
        valueDeserializer.addTrustedPackages("br.com.ecofy.ms_categorization.adapters.in.kafka.dto");

        return new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), valueDeserializer);
    }

    // Configura a fábrica de containers do @KafkaListener, incluindo error handler com backoff exponencial para retries.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CategorizationRequestMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, CategorizationRequestMessage> consumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, CategorizationRequestMessage>();
        factory.setConsumerFactory(consumerFactory);

        var backoff = new ExponentialBackOff(500L, 2.0);
        backoff.setMaxInterval(10_000L);

        factory.setCommonErrorHandler(new DefaultErrorHandler((rec, ex) -> { }, backoff));
        return factory;
    }

}
