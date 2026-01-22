package br.com.ecofy.ms_notification.config;

import br.com.ecofy.ms_notification.adapters.in.kafka.dto.BudgetAlertEventMessage;
import br.com.ecofy.ms_notification.adapters.in.kafka.dto.InsightCreatedEventMessage;
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

    private static final String TRUSTED_PACKAGE = "br.com.ecofy.ms_notification.adapters.in.kafka.dto";

    private final KafkaProperties kafkaProperties;
    private final JsonMapper jsonMapper;

    // Cria um ConsumerFactory tipado para consumir mensagens BudgetAlertEventMessage via JSON (JacksonJsonDeserializer).
    @Bean
    public ConsumerFactory<String, BudgetAlertEventMessage> budgetAlertConsumerFactory() {
        return buildJsonConsumerFactory(BudgetAlertEventMessage.class);
    }

    // Cria um ConsumerFactory tipado para consumir mensagens InsightCreatedEventMessage via JSON (JacksonJsonDeserializer).
    @Bean
    public ConsumerFactory<String, InsightCreatedEventMessage> insightCreatedConsumerFactory() {
        return buildJsonConsumerFactory(InsightCreatedEventMessage.class);
    }

    // Monta um ConsumerFactory genérico para payload JSON, removendo overrides spring.json.* e fixando deserialização segura/estável (sem type headers).
    private <T> ConsumerFactory<String, T> buildJsonConsumerFactory(Class<T> type) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        // Remove propriedades spring.json.* para evitar conflitos/overrides vindos de env/application.yml.
        props.remove("spring.json.trusted.packages");
        props.remove("spring.json.use.type.headers");
        props.remove("spring.json.value.default.type");
        props.remove("spring.json.type.mapping");
        props.remove("spring.json.remove.type.headers");

        // Define o deserializer da key como String quando não houver configuração explícita.
        props.putIfAbsent(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Instancia o deserializer JSON para o tipo esperado, usando JsonMapper (Jackson 3) e restringindo pacotes confiáveis.
        JacksonJsonDeserializer<T> valueDeserializer = new JacksonJsonDeserializer<>(type, jsonMapper);
        valueDeserializer.ignoreTypeHeaders();               // Ignora type headers para evitar acoplamento e riscos com headers de tipo.
        valueDeserializer.addTrustedPackages(TRUSTED_PACKAGE); // Restringe classes permitidas na desserialização (defesa contra gadget attacks).

        // Retorna um ConsumerFactory com key String e value JSON tipado, pronto para ser usado por factories de listeners.
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    // Cria a ListenerContainerFactory do consumer de budget alerts com ACK por record (commit após processamento bem-sucedido).
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BudgetAlertEventMessage> budgetAlertKafkaListenerContainerFactory(
            ConsumerFactory<String, BudgetAlertEventMessage> budgetAlertConsumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, BudgetAlertEventMessage>();
        factory.setConsumerFactory(budgetAlertConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    // Cria a ListenerContainerFactory do consumer de insight.created com ACK por record (commit após processamento bem-sucedido).
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
