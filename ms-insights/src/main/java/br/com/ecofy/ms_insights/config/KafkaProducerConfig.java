package br.com.ecofy.ms_insights.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@Slf4j
public class KafkaProducerConfig {

    // Cria e configura um KafkaTemplate<String,String> com serializers String, idempotência e parâmetros de confiabilidade para publicação de eventos.
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(KafkaProperties kafkaProperties) {
        Objects.requireNonNull(kafkaProperties, "kafkaProperties must not be null");

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

        // Serializers padrão (String -> JSON serializado externamente)
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Confiabilidade e consistência
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        props.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Retentativas e timeout (boa prática para eventos de domínio)
        props.putIfAbsent(ProducerConfig.RETRIES_CONFIG, 5);
        props.putIfAbsent(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
        props.putIfAbsent(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30_000);
        props.putIfAbsent(ProducerConfig.LINGER_MS_CONFIG, 5);

        // Segurança contra reordenação
        props.putIfAbsent(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        log.info(
                "[KafkaProducerConfig] KafkaTemplate | bootstrapServers={} | idempotence={}",
                kafkaProperties.getBootstrapServers(), // List<String> no Boot 4
                props.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG)
        );

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

}
