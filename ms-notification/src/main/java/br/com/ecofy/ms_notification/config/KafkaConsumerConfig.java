package br.com.ecofy.ms_notification.config;

import br.com.ecofy.ms_notification.adapters.in.kafka.dto.BudgetAlertEventMessage;
import br.com.ecofy.ms_notification.adapters.in.kafka.dto.InsightCreatedEventMessage;
import lombok.RequiredArgsConstructor;
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

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private static final String TRUSTED_PACKAGE = "br.com.ecofy.ms_notification.adapters.in.kafka.dto";

    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, BudgetAlertEventMessage> budgetAlertConsumerFactory() {
        return buildJsonConsumerFactory(BudgetAlertEventMessage.class);
    }

    @Bean
    public ConsumerFactory<String, InsightCreatedEventMessage> insightCreatedConsumerFactory() {
        return buildJsonConsumerFactory(InsightCreatedEventMessage.class);
    }

    private <T> ConsumerFactory<String, T> buildJsonConsumerFactory(Class<T> type) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        // Evita conflito: JsonDeserializer não pode ser configurado via props e via setters ao mesmo tempo.
        // Remova qualquer spring.json.* que possa ter vindo do application.yml/env.
        props.remove(JsonDeserializer.TRUSTED_PACKAGES);
        props.remove(JsonDeserializer.USE_TYPE_INFO_HEADERS);
        props.remove(JsonDeserializer.VALUE_DEFAULT_TYPE);
        props.remove(JsonDeserializer.TYPE_MAPPINGS);
        props.remove(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS);

        // Não é necessário setar VALUE_DESERIALIZER_CLASS_CONFIG quando passamos o deserializer por instância.
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JsonDeserializer<T> valueDeserializer = new JsonDeserializer<>(type);
        valueDeserializer.addTrustedPackages(TRUSTED_PACKAGE);

        // Garante que não dependa de headers de tipo (mais previsível).
        valueDeserializer.ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BudgetAlertEventMessage> budgetAlertKafkaListenerContainerFactory(
            ConsumerFactory<String, BudgetAlertEventMessage> budgetAlertConsumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, BudgetAlertEventMessage>();
        factory.setConsumerFactory(budgetAlertConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InsightCreatedEventMessage> insightCreatedKafkaListenerContainerFactory(
            ConsumerFactory<String, InsightCreatedEventMessage> insightCreatedConsumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, InsightCreatedEventMessage>();
        factory.setConsumerFactory(insightCreatedConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
