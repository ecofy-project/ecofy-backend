package br.com.ecofy.ms_categorization.config;

import br.com.ecofy.ms_categorization.adapters.correlation.CorrelationContext;
import br.com.ecofy.ms_categorization.core.application.exception.BusinessValidationException;
import br.com.ecofy.ms_categorization.core.application.exception.InvalidEventException;
import br.com.ecofy.ms_categorization.core.application.exception.UnsupportedEventVersionException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

// Configura o consumo Kafka com retentativas, DLT e concorrência.
@Configuration
@Slf4j
public class KafkaConfig {

    private static final int PARTITIONS = 3;

    public static final String DLT_ORIGINAL_TOPIC = "ecofy-dlt-original-topic";
    public static final String DLT_ORIGINAL_PARTITION = "ecofy-dlt-original-partition";
    public static final String DLT_ORIGINAL_OFFSET = "ecofy-dlt-original-offset";
    public static final String DLT_ORIGINAL_TIMESTAMP = "ecofy-dlt-original-timestamp";
    public static final String DLT_CONSUMER_GROUP = "ecofy-dlt-consumer-group";
    public static final String DLT_ERROR_CODE = "ecofy-dlt-error-code";
    public static final String DLT_ERROR_CATEGORY = "ecofy-dlt-error-category";
    public static final String DLT_EXCEPTION_TYPE = "ecofy-dlt-exception-type";
    public static final String DLT_TIMESTAMP = "ecofy-dlt-timestamp";

    // Configura o encaminhamento de mensagens rejeitadas para a DLT.
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> template,
                                                                       CategorizationProperties props,
                                                                       MeterRegistry meterRegistry) {
        String suffix = props.getKafka().getDltSuffix();

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> new TopicPartition(record.topic() + suffix, record.partition()));

        recoverer.setHeadersFunction((record, ex) -> enrichDltHeaders(record, ex, props, meterRegistry));

        recoverer.setExceptionHeadersCreator((kafkaHeaders, exception, isKey, headerNames) -> {
        });

        return recoverer;
    }

    // Aplica metadados sanitizados e preserva a correlação na DLT.
    private static Headers enrichDltHeaders(ConsumerRecord<?, ?> record,
                                            Exception ex,
                                            CategorizationProperties props,
                                            MeterRegistry meterRegistry) {
        Throwable cause = rootCause(ex);
        boolean permanent = isPermanent(cause);
        String category = permanent ? "permanent" : "transient";
        String errorCode = errorCodeOf(cause);

        Headers headers = record.headers();
        put(headers, DLT_ORIGINAL_TOPIC, record.topic());
        put(headers, DLT_ORIGINAL_PARTITION, String.valueOf(record.partition()));
        put(headers, DLT_ORIGINAL_OFFSET, String.valueOf(record.offset()));
        put(headers, DLT_ORIGINAL_TIMESTAMP, String.valueOf(record.timestamp()));
        put(headers, DLT_CONSUMER_GROUP, props.getKafka().getConsumerGroup());
        put(headers, DLT_ERROR_CODE, errorCode);
        put(headers, DLT_ERROR_CATEGORY, category);
        put(headers, DLT_EXCEPTION_TYPE, cause.getClass().getSimpleName());
        put(headers, DLT_TIMESTAMP, Instant.now().toString());

        if (headers.lastHeader(CorrelationContext.KAFKA_HEADER) == null) {
            put(headers, CorrelationContext.KAFKA_HEADER, CorrelationContext.currentCorrelationIdOrGenerate());
        }

        meterRegistry.counter("ecofy.categorization.dlt.total",
                "error_category", category,
                "reason", errorCode).increment();

        log.error("[KafkaConfig] - [dlt] -> mensagem para DLT topic={} partition={} offset={} errorCode={} category={}",
                record.topic(), record.partition(), record.offset(), errorCode, category);

        return headers;
    }

    // Configura retentativas e classifica falhas permanentes para envio direto à DLT.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer,
                                                 CategorizationProperties props,
                                                 MeterRegistry meterRegistry) {
        CategorizationProperties.Kafka.Retry retry = props.getKafka().getRetry();

        ExponentialBackOff backoff = new ExponentialBackOff(
                retry.getInitialInterval().toMillis(), retry.getMultiplier());
        backoff.setMaxInterval(retry.getMaxInterval().toMillis());
        backoff.setMaxAttempts(Math.max(0, retry.getMaxAttempts() - 1));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);

        handler.addNotRetryableExceptions(
                InvalidEventException.class,
                UnsupportedEventVersionException.class,
                BusinessValidationException.class,
                IllegalArgumentException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class,
                org.springframework.messaging.converter.MessageConversionException.class);

        handler.setRetryListeners(new org.springframework.kafka.listener.RetryListener() {

            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int attempt) {
                meterRegistry.counter("ecofy.categorization.kafka.retry.total", "topic", record.topic()).increment();
                log.warn("[KafkaConfig] - [retry] -> attempt={} topic={} partition={} offset={} type={}",
                        attempt, record.topic(), record.partition(), record.offset(), ex.getClass().getSimpleName());
            }

            @Override
            public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
                meterRegistry.counter("ecofy.categorization.dlt.publish.failed.total").increment();
                log.error("[KafkaConfig] - [recoveryFailed] -> FALHA ao publicar na DLT topic={} partition={} offset={} "
                                + "originalType={} dltError={}",
                        record.topic(), record.partition(), record.offset(),
                        original.getClass().getSimpleName(), failure.getMessage());
            }
        });

        return handler;
    }

    // Configura o listener com tratamento de erros e concorrência.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler,
            CategorizationProperties props
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(props.getKafka().getConcurrency());
        return factory;
    }

    @Bean
    public NewTopic categorizationRequestTopic(CategorizationProperties props) {
        return TopicBuilder.name(props.getTopics().getCategorizationRequest())
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }

    // Declara a DLT com o mesmo particionamento do tópico principal.
    @Bean
    public NewTopic categorizationRequestDltTopic(CategorizationProperties props) {
        return TopicBuilder.name(props.getTopics().getCategorizationRequest() + props.getKafka().getDltSuffix())
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionCategorizedTopic(CategorizationProperties props) {
        return TopicBuilder.name(props.getTopics().getTransactionCategorized())
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic categorizationAppliedTopic(CategorizationProperties props) {
        return TopicBuilder.name(props.getTopics().getCategorizationApplied())
                .partitions(PARTITIONS)
                .replicas(1)
                .build();
    }

    private static boolean isPermanent(Throwable cause) {
        return cause instanceof InvalidEventException
                || cause instanceof UnsupportedEventVersionException
                || cause instanceof BusinessValidationException
                || cause instanceof IllegalArgumentException
                || cause instanceof org.springframework.kafka.support.serializer.DeserializationException
                || cause instanceof org.springframework.messaging.converter.MessageConversionException;
    }

    private static String errorCodeOf(Throwable cause) {
        if (cause instanceof UnsupportedEventVersionException) {
            return "UNSUPPORTED_EVENT_VERSION";
        }
        if (cause instanceof InvalidEventException) {
            return "INVALID_EVENT";
        }
        if (cause instanceof BusinessValidationException) {
            return "BUSINESS_VALIDATION_FAILED";
        }
        return cause.getClass().getSimpleName();
    }

    // Resolve a causa relevante para classificar a falha.
    private static Throwable rootCause(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (isPermanent(current)) {
                return current;
            }
            if (current.getCause() == null || current.getCause() == current) {
                return current;
            }
            current = current.getCause();
        }
        return ex;
    }

    private static void put(Headers headers, String key, String value) {
        headers.remove(key);
        headers.add(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
