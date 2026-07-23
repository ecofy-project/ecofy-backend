package br.com.ecofy.ms_notification.config;

import br.com.ecofy.ms_notification.adapters.in.kafka.dto.BudgetAlertEventMessage;
import br.com.ecofy.ms_notification.adapters.in.kafka.dto.InsightCreatedEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

// Configura consumidores Kafka com desserialização segura, retentativas e DLT.
@Configuration
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private static final String TRUSTED_PACKAGE =
            "br.com.ecofy.ms_notification.adapters.in.kafka.dto";
    private static final String DLT_SUFFIX = ".dlt";

    private final KafkaProperties kafkaProperties;
    private final JsonMapper jsonMapper;

    // Configura o consumo de alertas orçamentários com desserialização JSON tipada.
    @Bean
    public ConsumerFactory<String, BudgetAlertEventMessage> budgetAlertConsumerFactory() {
        return buildJsonConsumerFactory(BudgetAlertEventMessage.class);
    }

    // Configura o consumo de insights criados com desserialização JSON tipada.
    @Bean
    public ConsumerFactory<String, InsightCreatedEventMessage> insightCreatedConsumerFactory() {
        return buildJsonConsumerFactory(InsightCreatedEventMessage.class);
    }

    // Centraliza a criação de consumidores JSON com tratamento seguro de falhas.
    private <T> ConsumerFactory<String, T> buildJsonConsumerFactory(Class<T> type) {
        Map<String, Object> props =
                new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.remove("spring.json.trusted.packages");
        props.remove("spring.json.use.type.headers");
        props.remove("spring.json.value.default.type");
        props.remove("spring.json.type.mapping");
        props.remove("spring.json.remove.type.headers");

        props.putIfAbsent(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        JacksonJsonDeserializer<T> valueDeserializer =
                new JacksonJsonDeserializer<>(type, jsonMapper);
        valueDeserializer.ignoreTypeHeaders();
        valueDeserializer.addTrustedPackages(TRUSTED_PACKAGE);

        var errorHandlingValue =
                new ErrorHandlingDeserializer<>(valueDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                errorHandlingValue
        );
    }

    // Configura retentativas e encaminha falhas não recuperáveis para a DLT.
    @Bean
    public DefaultErrorHandler notificationKafkaErrorHandler(
            KafkaTemplateBytesProvider dltTemplateProvider
    ) {
        var recoverer = new DeadLetterPublishingRecoverer(
                dltTemplateProvider.template(),
                (record, ex) -> new TopicPartition(
                        record.topic() + DLT_SUFFIX,
                        record.partition()
                )
        );

        recoverer.setExceptionHeadersCreator(
                (headers, exception, isKey, headerNames) -> {
                    var info = headerNames.getExceptionInfo();
                    headers.add(
                            info.getExceptionFqcn(),
                            exception.getClass().getName().getBytes()
                    );
                    String msg = exception.getMessage();
                    headers.add(
                            info.getExceptionMessage(),
                            (msg == null ? "" : msg).getBytes()
                    );
                }
        );

        var backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxElapsedTime(60_000L);

        var handler = new DefaultErrorHandler(recoverer, backOff);
        handler.setLogLevel(KafkaException.Level.ERROR);
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class,
                NullPointerException.class,
                ClassCastException.class
        );

        return handler;
    }

    // Encapsula o template binário utilizado na publicação para a DLT.
    public record KafkaTemplateBytesProvider(
            KafkaOperations<byte[], byte[]> template
    ) {
    }

    // Resolve a injeção do template binário utilizado pela DLT.
    @Bean
    public KafkaTemplateBytesProvider kafkaTemplateBytesProvider(
            org.springframework.kafka.core.KafkaTemplate<byte[], byte[]> dltKafkaTemplate
    ) {
        return new KafkaTemplateBytesProvider(dltKafkaTemplate);
    }

    // Configura o listener de alertas com confirmação por registro e recuperação via DLT.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BudgetAlertEventMessage>
    budgetAlertKafkaListenerContainerFactory(
            ConsumerFactory<String, BudgetAlertEventMessage> budgetAlertConsumerFactory,
            DefaultErrorHandler notificationKafkaErrorHandler
    ) {
        var factory =
                new ConcurrentKafkaListenerContainerFactory<String, BudgetAlertEventMessage>();
        factory.setConsumerFactory(budgetAlertConsumerFactory);
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(notificationKafkaErrorHandler);
        return factory;
    }

    // Configura o listener de insights com confirmação por registro e recuperação via DLT.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InsightCreatedEventMessage>
    insightCreatedKafkaListenerContainerFactory(
            ConsumerFactory<String, InsightCreatedEventMessage> insightCreatedConsumerFactory,
            DefaultErrorHandler notificationKafkaErrorHandler
    ) {
        var factory =
                new ConcurrentKafkaListenerContainerFactory<String, InsightCreatedEventMessage>();
        factory.setConsumerFactory(insightCreatedConsumerFactory);
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(notificationKafkaErrorHandler);
        return factory;
    }
}