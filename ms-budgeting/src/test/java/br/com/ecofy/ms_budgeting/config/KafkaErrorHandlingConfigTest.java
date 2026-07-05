package br.com.ecofy.ms_budgeting.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaErrorHandlingConfigTest {

    private static final String SOURCE_TOPIC = "budgeting.categorized-transaction";
    private static final String DLT_TOPIC = SOURCE_TOPIC + ".DLT";
    private static final int PARTITION = 2;
    private static final long OFFSET = 100L;
    private static final Instant NOW = Instant.parse("2026-06-25T10:30:00Z");

    @Test
    void shouldCreateKafkaErrorHandlingConfigInstance() {
        KafkaErrorHandlingConfig config = new KafkaErrorHandlingConfig();

        assertNotNull(config);
    }

    @Test
    void shouldCreateDeadLetterPublishingRecoverer() {
        KafkaErrorHandlingConfig config = new KafkaErrorHandlingConfig();
        KafkaTemplate<String, Object> template = kafkaTemplate();

        DeadLetterPublishingRecoverer recoverer =
                config.deadLetterPublishingRecoverer(template);

        assertNotNull(recoverer);
    }

    @Test
    void shouldPublishFailedRecordToDeadLetterTopicUsingOriginalPartition() {
        KafkaErrorHandlingConfig config = new KafkaErrorHandlingConfig();
        KafkaTemplate<String, Object> template = kafkaTemplate();

        doReturn(successfulSend(DLT_TOPIC, PARTITION))
                .when(template)
                .send(anyProducerRecord());

        DeadLetterPublishingRecoverer recoverer =
                config.deadLetterPublishingRecoverer(template);

        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                SOURCE_TOPIC,
                PARTITION,
                OFFSET,
                "key-001",
                "payload-001"
        );

        RuntimeException exception = new RuntimeException("processing failed");

        recoverer.accept(record, exception);

        ArgumentCaptor<ProducerRecord<String, Object>> captor =
                producerRecordCaptor();

        verify(template).send(captor.capture());

        ProducerRecord<String, Object> producerRecord = captor.getValue();

        assertNotNull(producerRecord);
        assertEquals(DLT_TOPIC, producerRecord.topic());
        assertEquals(PARTITION, producerRecord.partition());
        assertEquals("key-001", producerRecord.key());
        assertEquals("payload-001", producerRecord.value());
    }

    @Test
    void shouldCreateDefaultErrorHandler() {
        KafkaErrorHandlingConfig config = new KafkaErrorHandlingConfig();
        DeadLetterPublishingRecoverer recoverer =
                mock(DeadLetterPublishingRecoverer.class);

        DefaultErrorHandler handler = config.kafkaErrorHandler(recoverer);

        assertNotNull(handler);
    }

    @Test
    void shouldConfigureRetryListenerAndExecuteIt() throws Exception {
        KafkaErrorHandlingConfig config = new KafkaErrorHandlingConfig();
        DeadLetterPublishingRecoverer recoverer =
                mock(DeadLetterPublishingRecoverer.class);

        DefaultErrorHandler handler = config.kafkaErrorHandler(recoverer);

        RetryListener[] listeners = retryListeners(handler);

        assertNotNull(listeners);
        assertEquals(1, listeners.length);

        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                SOURCE_TOPIC,
                PARTITION,
                OFFSET,
                "key-001",
                "payload-001"
        );

        RuntimeException exception = new RuntimeException("retry failed");

        assertDoesNotThrow(() ->
                listeners[0].failedDelivery(record, exception, 2)
        );
    }

    @Test
    void shouldHaveConfigurationAnnotation() {
        Configuration annotation =
                KafkaErrorHandlingConfig.class.getAnnotation(Configuration.class);

        assertNotNull(annotation);
    }

    @Test
    void shouldHaveBeanAnnotationOnDeadLetterPublishingRecovererMethod()
            throws Exception {
        Method method = KafkaErrorHandlingConfig.class.getDeclaredMethod(
                "deadLetterPublishingRecoverer",
                KafkaTemplate.class
        );

        Bean bean = method.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertEquals(DeadLetterPublishingRecoverer.class, method.getReturnType());
    }

    @Test
    void shouldHaveBeanAnnotationOnKafkaErrorHandlerMethod()
            throws Exception {
        Method method = KafkaErrorHandlingConfig.class.getDeclaredMethod(
                "kafkaErrorHandler",
                DeadLetterPublishingRecoverer.class
        );

        Bean bean = method.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertEquals(DefaultErrorHandler.class, method.getReturnType());
    }

    @SuppressWarnings("unchecked")
    private static KafkaTemplate<String, Object> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }

    @SuppressWarnings("unchecked")
    private static ProducerRecord<String, Object> anyProducerRecord() {
        return (ProducerRecord<String, Object>) any(ProducerRecord.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<ProducerRecord<String, Object>> producerRecordCaptor() {
        return ArgumentCaptor.forClass((Class) ProducerRecord.class);
    }

    private static CompletableFuture<SendResult<String, Object>> successfulSend(
            String topic,
            int partition
    ) {
        ProducerRecord<String, Object> producerRecord =
                new ProducerRecord<>(topic, partition, "key-001", "payload-001");

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(topic, partition),
                0L,
                0,
                NOW.toEpochMilli(),
                10,
                20
        );

        SendResult<String, Object> sendResult =
                new SendResult<>(producerRecord, metadata);

        return CompletableFuture.completedFuture(sendResult);
    }

    private static RetryListener[] retryListeners(DefaultErrorHandler handler)
            throws Exception {
        RetryListener[] listeners = findRetryListeners(
                handler,
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()),
                0
        );

        if (listeners == null || listeners.length == 0) {
            throw new AssertionError("RetryListener field not found in DefaultErrorHandler object graph");
        }

        return listeners;
    }

    private static RetryListener[] findRetryListeners(
            Object target,
            java.util.Set<Object> visited,
            int depth
    ) throws Exception {
        if (target == null || depth > 8 || visited.contains(target)) {
            return null;
        }

        visited.add(target);

        if (target instanceof RetryListener listener) {
            return new RetryListener[]{listener};
        }

        if (target instanceof RetryListener[] listeners) {
            return listeners;
        }

        if (target instanceof java.util.Collection<?> collection
                && !collection.isEmpty()
                && collection.stream().allMatch(RetryListener.class::isInstance)) {
            return collection.toArray(new RetryListener[0]);
        }

        Class<?> current = target.getClass();

        while (current != null) {
            for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                Object value = readFieldSafely(target, field);

                RetryListener[] direct = extractRetryListeners(value);

                if (direct != null && direct.length > 0) {
                    return direct;
                }

                if (shouldInspectFieldValue(value)) {
                    RetryListener[] nested = findRetryListeners(
                            value,
                            visited,
                            depth + 1
                    );

                    if (nested != null && nested.length > 0) {
                        return nested;
                    }
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private static RetryListener[] extractRetryListeners(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof RetryListener listener) {
            return new RetryListener[]{listener};
        }

        if (value instanceof RetryListener[] listeners) {
            return listeners;
        }

        if (value instanceof java.util.Collection<?> collection
                && !collection.isEmpty()
                && collection.stream().allMatch(RetryListener.class::isInstance)) {
            return collection.toArray(new RetryListener[0]);
        }

        return null;
    }

    private static Object readFieldSafely(Object target, java.lang.reflect.Field field)
            throws IllegalAccessException {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean shouldInspectFieldValue(Object value) {
        if (value == null) {
            return false;
        }

        Class<?> type = value.getClass();

        if (type.isPrimitive()
                || type.isEnum()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type) {
            return false;
        }

        Package pkg = type.getPackage();

        if (pkg == null) {
            return false;
        }

        String packageName = pkg.getName();

        return packageName.startsWith("org.springframework.kafka")
                || packageName.startsWith("org.springframework.util")
                || packageName.startsWith("java.util");
    }
}