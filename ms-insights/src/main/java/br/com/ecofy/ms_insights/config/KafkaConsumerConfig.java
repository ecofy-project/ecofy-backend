package br.com.ecofy.ms_insights.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Configura o consumo de eventos Kafka pelo serviço.
@Configuration
@Slf4j
public class KafkaConsumerConfig {

    // Configura listeners com confirmação por registro, concorrência e retentativas.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory(
            KafkaProperties kafkaProperties,
            InsightsProperties insightsProperties
    ) {
        Objects.requireNonNull(
                kafkaProperties,
                "kafkaProperties must not be null"
        );
        Objects.requireNonNull(
                insightsProperties,
                "insightsProperties must not be null"
        );

        Map<String, Object> props = new HashMap<>(
                kafkaProperties.buildConsumerProperties()
        );

        props.putIfAbsent(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        props.putIfAbsent(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        props.putIfAbsent(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );
        props.putIfAbsent(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );
        props.putIfAbsent(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                10
        );
        props.putIfAbsent(
                ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                300_000
        );
        props.putIfAbsent(
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
                15_000
        );
        props.putIfAbsent(
                ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
                5_000
        );

        var factory =
                new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(
                new DefaultKafkaConsumerFactory<>(props)
        );

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);

        int concurrency = insightsProperties.kafka() != null
                ? insightsProperties.kafka().concurrency()
                : 3;
        factory.setConcurrency(concurrency);

        var errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1_000L, 2L)
        );
        errorHandler.setLogLevel(
                org.springframework.kafka.KafkaException.Level.ERROR
        );
        factory.setCommonErrorHandler(errorHandler);

        log.info(
                "[KafkaConsumerConfig] KafkaListenerContainerFactory | bootstrapServers={} | groupId={} | ackMode={}",
                kafkaProperties.getBootstrapServers(),
                props.get(ConsumerConfig.GROUP_ID_CONFIG),
                factory.getContainerProperties().getAckMode()
        );

        return factory;
    }
}
