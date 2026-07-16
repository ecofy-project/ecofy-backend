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

@Configuration
@Slf4j
public class KafkaConsumerConfig {

    // Cria e configura a factory de listeners Kafka (String/String) com propriedades de consumo estável, ack por record e concorrência controlada.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        Objects.requireNonNull(kafkaProperties, "kafkaProperties must not be null");

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.putIfAbsent(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Boas práticas de consumo estável
        props.putIfAbsent(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.putIfAbsent(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.putIfAbsent(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300_000); // 5 min
        props.putIfAbsent(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15_000);
        props.putIfAbsent(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 5_000);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));

        // Consistência: commit só após processamento bem-sucedido
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // Concorrência: default seguro (depois você pode parametrizar)
        factory.setConcurrency(1);

        // Correção Dia 8 (item #6): antes os consumers engoliam Exception (sem retry) e uma falha
        // transitória era tratada como "processada". Agora exceções relançadas pelo listener passam
        // por retry com backoff (2 re-tentativas, 1s). Após esgotar, o DefaultErrorHandler loga e segue
        // (sem DLT — publicação em Dead Letter Topic fica como próximo passo documentado).
        var errorHandler = new DefaultErrorHandler(new FixedBackOff(1_000L, 2L));
        errorHandler.setLogLevel(org.springframework.kafka.KafkaException.Level.ERROR);
        factory.setCommonErrorHandler(errorHandler);

        log.info(
                "[KafkaConsumerConfig] KafkaListenerContainerFactory | bootstrapServers={} | groupId={} | ackMode={}",
                kafkaProperties.getBootstrapServers(), // List<String> no Boot 4
                props.get(ConsumerConfig.GROUP_ID_CONFIG),
                factory.getContainerProperties().getAckMode()
        );

        return factory;
    }

}
