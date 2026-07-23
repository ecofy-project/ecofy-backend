package br.com.ecofy.ms_ingestion.config;

import br.com.ecofy.ms_ingestion.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.ms_ingestion.core.application.exception.EmptyTransactionsPayloadException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionErrorCode;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import br.com.ecofy.ms_ingestion.core.application.exception.InvalidKafkaMessageException;
import br.com.ecofy.ms_ingestion.core.application.exception.UnsupportedEventVersionException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

// Configura retentativas e encaminhamento de falhas para a DLT.
@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {

    public static final String DLT_ORIGINAL_TOPIC = "ecofy-dlt-original-topic";
    public static final String DLT_ORIGINAL_PARTITION = "ecofy-dlt-original-partition";
    public static final String DLT_ORIGINAL_OFFSET = "ecofy-dlt-original-offset";
    public static final String DLT_CONSUMER_GROUP = "ecofy-dlt-consumer-group";
    public static final String DLT_ERROR_CODE = "ecofy-dlt-error-code";
    public static final String DLT_ERROR_CATEGORY = "ecofy-dlt-error-category";
    public static final String DLT_EXCEPTION_TYPE = "ecofy-dlt-exception-type";
    public static final String DLT_ATTEMPTS = "ecofy-dlt-attempts";
    public static final String DLT_TIMESTAMP = "ecofy-dlt-timestamp";

    private final IngestionProperties properties;
    private final MeterRegistry meterRegistry;

    public KafkaErrorHandlingConfig(IngestionProperties properties, MeterRegistry meterRegistry) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    // Configura o encaminhamento das mensagens rejeitadas para a DLT.
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> template,
                                                                       IngestionProperties props) {
        String suffix = props.getKafka().getDltSuffix();

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> new TopicPartition(record.topic() + suffix, record.partition()));

        recoverer.setHeadersFunction((record, ex) -> enrichHeaders(record, ex));
        return recoverer;
    }

    // Aplica metadados seguros e preserva a correlação na DLT.
    private Headers enrichHeaders(ConsumerRecord<?, ?> record, Exception ex) {
        Throwable cause = rootIngestionCause(ex);

        String errorCode = cause instanceof IngestionException ingestionEx && ingestionEx.getErrorCode() != null
                ? ingestionEx.getErrorCode().getCode()
                : IngestionErrorCode.INTERNAL_INGESTION_ERROR.getCode();

        String category = isPermanent(cause) ? "permanent" : "transient";

        Headers headers = record.headers();
        put(headers, DLT_ORIGINAL_TOPIC, record.topic());
        put(headers, DLT_ORIGINAL_PARTITION, String.valueOf(record.partition()));
        put(headers, DLT_ORIGINAL_OFFSET, String.valueOf(record.offset()));
        put(headers, DLT_CONSUMER_GROUP, properties.getKafka().getConsumerGroup());
        put(headers, DLT_ERROR_CODE, errorCode);
        put(headers, DLT_ERROR_CATEGORY, category);
        put(headers, DLT_EXCEPTION_TYPE, cause.getClass().getSimpleName());
        put(headers, DLT_TIMESTAMP, Instant.now().toString());

        if (headers.lastHeader(CorrelationId.KAFKA_HEADER) == null) {
            put(headers, CorrelationId.KAFKA_HEADER, CorrelationId.currentOrGenerate());
        }

        meterRegistry.counter("ecofy.ingestion.dlt.total",
                "error_category", category,
                "error_code", errorCode).increment();

        log.error("[KafkaErrorHandlingConfig] - [dlt] -> Mensagem enviada para DLT topic={} partition={} offset={} "
                        + "errorCode={} category={}",
                record.topic(), record.partition(), record.offset(), errorCode, category);

        return headers;
    }

    // Configura o backoff e exclui falhas permanentes das retentativas.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer,
                                                 IngestionProperties props) {
        IngestionProperties.KafkaRetry retry = props.getKafka().getRetry();

        ExponentialBackOff backOff = new ExponentialBackOff(
                retry.getInitialInterval().toMillis(), retry.getMultiplier());
        backOff.setMaxInterval(retry.getMaxInterval().toMillis());
        backOff.setMaxAttempts(Math.max(0, retry.getMaxAttempts() - 1));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        handler.addNotRetryableExceptions(
                InvalidKafkaMessageException.class,
                UnsupportedEventVersionException.class,
                EmptyTransactionsPayloadException.class,
                IllegalArgumentException.class);

        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            meterRegistry.counter("ecofy.ingestion.kafka.retry.total", "topic", record.topic()).increment();
            log.warn("[KafkaErrorHandlingConfig] - [retry] -> attempt={} topic={} partition={} offset={} type={}",
                    deliveryAttempt, record.topic(), record.partition(), record.offset(),
                    ex.getClass().getSimpleName());
        });

        return handler;
    }

    private static boolean isPermanent(Throwable cause) {
        return cause instanceof InvalidKafkaMessageException
                || cause instanceof UnsupportedEventVersionException
                || cause instanceof EmptyTransactionsPayloadException;
    }

    // Resolve a causa relevante para classificar a falha.
    private static Throwable rootIngestionCause(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof IngestionException) {
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
