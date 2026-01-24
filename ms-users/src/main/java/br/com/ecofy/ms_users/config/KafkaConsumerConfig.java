package br.com.ecofy.ms_users.config;

import br.com.ecofy.ms_users.adapters.in.kafka.dto.AuthUserCreatedEventMessage;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private static final String TRUSTED_PACKAGE = "br.com.ecofy.ms_users.adapters.in.kafka.dto";

    private final KafkaProperties kafkaProperties;
    private final JsonMapper jsonMapper;

    // Cria um ConsumerFactory configurado para desserializar eventos AuthUserCreatedEventMessage a partir do Kafka usando Jackson.
    @Bean
    public ConsumerFactory<String, AuthUserCreatedEventMessage> authUserCreatedConsumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        // Evita conflitos caso algo venha do application.yml/env (spring.json.*)
        props.remove("spring.json.trusted.packages");
        props.remove("spring.json.use.type.headers");
        props.remove("spring.json.value.default.type");
        props.remove("spring.json.type.mapping");
        props.remove("spring.json.remove.type.headers");

        // Key como String
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Jackson 3 / Spring Kafka 4
        JacksonJsonDeserializer<AuthUserCreatedEventMessage> valueDeserializer =
                new JacksonJsonDeserializer<>(AuthUserCreatedEventMessage.class, jsonMapper);

        valueDeserializer.ignoreTypeHeaders();
        valueDeserializer.addTrustedPackages(TRUSTED_PACKAGE);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    /**
     * Esse bean precisa ter o mesmo nome usado no seu @KafkaListener(containerFactory=...).
     * Você está usando "kafkaListenerContainerFactory" no consumer, então mantemos esse nome.
     */
    // Cria o ConcurrentKafkaListenerContainerFactory usado pelos @KafkaListener, definindo ConsumerFactory e estratégia de ack/commit.
    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, AuthUserCreatedEventMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, AuthUserCreatedEventMessage> authUserCreatedConsumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, AuthUserCreatedEventMessage>();
        factory.setConsumerFactory(authUserCreatedConsumerFactory);

        // Commit apenas após processamento bem-sucedido
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }

}
