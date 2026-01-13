package br.com.ecofy.ms_users.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Configuration
public class KafkaProducerConfig {

    private static final int DEFAULT_RETRIES = 3;
    private static final boolean ENABLE_IDEMPOTENCE = true;

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
        Objects.requireNonNull(kafkaProperties, "kafkaProperties must not be null");

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // delivery guarantees (broker >= 0.11)
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, DEFAULT_RETRIES);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, ENABLE_IDEMPOTENCE);

        // recomendável quando idempotência está ativa (mantém ordering por partition)
        props.putIfAbsent(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // evita falhas por serialização de type headers em consumers heterogêneos
        props.putIfAbsent(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        log.info(
                "[KafkaProducerConfig] - [producerFactory] -> acks={} retries={} idempotence={} addTypeHeaders={}",
                props.get(ProducerConfig.ACKS_CONFIG),
                props.get(ProducerConfig.RETRIES_CONFIG),
                props.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG),
                props.get(JsonSerializer.ADD_TYPE_INFO_HEADERS)
        );

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        Objects.requireNonNull(producerFactory, "producerFactory must not be null");

        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);

        // logs de publish (sucesso/erro)
        template.setProducerListener(new ProducerListener<>() {
            @Override
            public void onSuccess(org.apache.kafka.clients.producer.ProducerRecord<String, Object> record,
                                  org.apache.kafka.clients.producer.RecordMetadata metadata) {
                log.debug(
                        "[KafkaProducerConfig] - [kafkaTemplate] -> published topic={} key={} partition={} offset={}",
                        metadata.topic(),
                        safeKey(record.key()),
                        metadata.partition(),
                        metadata.offset()
                );
            }

            @Override
            public void onError(org.apache.kafka.clients.producer.ProducerRecord<String, Object> record,
                                org.apache.kafka.clients.producer.RecordMetadata metadata,
                                Exception exception) {
                log.error(
                        "[KafkaProducerConfig] - [kafkaTemplate] -> publish failed topic={} key={} reason={}",
                        record.topic(),
                        safeKey(record.key()),
                        exception.getMessage(),
                        exception
                );
            }
        });

        log.info("[KafkaProducerConfig] - [kafkaTemplate] -> KafkaTemplate configured");
        return template;
    }

    private static String safeKey(String key) {
        if (key == null || key.isBlank()) return "<empty>";
        if (key.length() <= 8) return "***";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
