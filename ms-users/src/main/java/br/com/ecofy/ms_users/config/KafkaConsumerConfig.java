package br.com.ecofy.ms_users.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    private static final String TRUSTED_PACKAGES = "br.com.ecofy.ms_users.adapters.in.kafka.dto";
    private static final int DEFAULT_CONCURRENCY = 1;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties kafkaProperties) {
        Objects.requireNonNull(kafkaProperties, "kafkaProperties must not be null");

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));

        // Importante: não configure JsonDeserializer via "props" se você vai configurar no objeto deserializer.
        // Mantenha apenas o key deserializer aqui; o value deserializer será o "valueDeserializer" abaixo.
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Configuração via setters/objeto (única fonte de verdade)
        JsonDeserializer<Object> valueDeserializer = new JsonDeserializer<>(Object.class, objectMapper, false);
        valueDeserializer.addTrustedPackages(TRUSTED_PACKAGES);
        valueDeserializer.setUseTypeHeaders(false);      // sem type headers
        valueDeserializer.setRemoveTypeHeaders(true);    // remove headers se existirem

        log.info(
                "[KafkaConsumerConfig] - [consumerFactory] -> trustedPackages={} useTypeHeaders={}",
                TRUSTED_PACKAGES,
                false
        );

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaProperties kafkaProperties
    ) {
        Objects.requireNonNull(consumerFactory, "consumerFactory must not be null");
        Objects.requireNonNull(kafkaProperties, "kafkaProperties must not be null");

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        Integer configured = (kafkaProperties.getListener() != null)
                ? kafkaProperties.getListener().getConcurrency()
                : null;

        int concurrency = (configured == null || configured < 1) ? DEFAULT_CONCURRENCY : configured;
        factory.setConcurrency(concurrency);

        log.info(
                "[KafkaConsumerConfig] - [kafkaListenerContainerFactory] -> ackMode={} concurrency={}",
                factory.getContainerProperties().getAckMode(),
                concurrency
        );

        return factory;
    }
}
