package br.com.ecofy.ms_budgeting.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class KafkaListenerConfigTest {

    @Test
    void shouldCreateKafkaListenerConfigInstance() {
        KafkaListenerConfig config = new KafkaListenerConfig();

        assertNotNull(config);
    }

    @Test
    void shouldCreateFactoryWithConfiguredConcurrency() throws Exception {
        KafkaListenerConfig config = new KafkaListenerConfig();

        setConcurrency(config, 5);

        ConsumerFactory<Object, Object> consumerFactory = consumerFactory();
        DefaultErrorHandler errorHandler = errorHandler();
        KafkaProperties kafkaProperties = new KafkaProperties();

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.budgetingKafkaListenerContainerFactory(
                        consumerFactory,
                        errorHandler,
                        kafkaProperties
                );

        assertNotNull(factory);
        assertEquals(5, readField(factory, "concurrency"));
        assertSame(consumerFactory, readFieldInHierarchy(factory, "consumerFactory"));
        assertSame(errorHandler, readFieldInHierarchy(factory, "commonErrorHandler"));
    }

    @Test
    void shouldForceConcurrencyToOneWhenConfiguredConcurrencyIsZero() throws Exception {
        KafkaListenerConfig config = new KafkaListenerConfig();

        setConcurrency(config, 0);

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.budgetingKafkaListenerContainerFactory(
                        consumerFactory(),
                        errorHandler(),
                        new KafkaProperties()
                );

        assertNotNull(factory);
        assertEquals(1, readField(factory, "concurrency"));
    }

    @Test
    void shouldForceConcurrencyToOneWhenConfiguredConcurrencyIsNegative() throws Exception {
        KafkaListenerConfig config = new KafkaListenerConfig();

        setConcurrency(config, -3);

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.budgetingKafkaListenerContainerFactory(
                        consumerFactory(),
                        errorHandler(),
                        new KafkaProperties()
                );

        assertNotNull(factory);
        assertEquals(1, readField(factory, "concurrency"));
    }

    @Test
    void shouldKeepConcurrencyAsOneWhenConfiguredConcurrencyIsOne() throws Exception {
        KafkaListenerConfig config = new KafkaListenerConfig();

        setConcurrency(config, 1);

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.budgetingKafkaListenerContainerFactory(
                        consumerFactory(),
                        errorHandler(),
                        new KafkaProperties()
                );

        assertNotNull(factory);
        assertEquals(1, readField(factory, "concurrency"));
    }

    @Test
    void shouldApplyAckModeWhenKafkaPropertiesListenerHasAckMode() throws Exception {
        KafkaListenerConfig config = new KafkaListenerConfig();

        setConcurrency(config, 3);

        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.getListener().setAckMode(ContainerProperties.AckMode.MANUAL);

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.budgetingKafkaListenerContainerFactory(
                        consumerFactory(),
                        errorHandler(),
                        kafkaProperties
                );

        assertNotNull(factory);
        assertEquals(
                ContainerProperties.AckMode.MANUAL,
                factory.getContainerProperties().getAckMode()
        );
    }

    @Test
    void shouldApplyPollTimeoutWhenKafkaPropertiesListenerHasPollTimeout() throws Exception {
        KafkaListenerConfig config = new KafkaListenerConfig();

        setConcurrency(config, 3);

        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.getListener().setPollTimeout(Duration.ofSeconds(15));

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.budgetingKafkaListenerContainerFactory(
                        consumerFactory(),
                        errorHandler(),
                        kafkaProperties
                );

        assertNotNull(factory);
        assertEquals(
                Duration.ofSeconds(15).toMillis(),
                factory.getContainerProperties().getPollTimeout()
        );
    }

    @Test
    void shouldApplyAckModeAndPollTimeoutWhenBothAreConfigured() throws Exception {
        KafkaListenerConfig config = new KafkaListenerConfig();

        setConcurrency(config, 4);

        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.getListener().setAckMode(ContainerProperties.AckMode.RECORD);
        kafkaProperties.getListener().setPollTimeout(Duration.ofMillis(2500));

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.budgetingKafkaListenerContainerFactory(
                        consumerFactory(),
                        errorHandler(),
                        kafkaProperties
                );

        assertNotNull(factory);
        assertEquals(4, readField(factory, "concurrency"));
        assertEquals(
                ContainerProperties.AckMode.RECORD,
                factory.getContainerProperties().getAckMode()
        );
        assertEquals(
                2500L,
                factory.getContainerProperties().getPollTimeout()
        );
    }

    @Test
    void shouldNotApplyAckModeWhenKafkaPropertiesListenerAckModeIsNull() throws Exception {
        KafkaListenerConfig config = new KafkaListenerConfig();

        setConcurrency(config, 3);

        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.getListener().setAckMode(null);

        ContainerProperties.AckMode defaultAckMode =
                new ConcurrentKafkaListenerContainerFactory<>()
                        .getContainerProperties()
                        .getAckMode();

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.budgetingKafkaListenerContainerFactory(
                        consumerFactory(),
                        errorHandler(),
                        kafkaProperties
                );

        assertNotNull(factory);
        assertEquals(defaultAckMode, factory.getContainerProperties().getAckMode());
    }

    @Test
    void shouldNotApplyPollTimeoutWhenKafkaPropertiesListenerPollTimeoutIsNull() throws Exception {
        KafkaListenerConfig config = new KafkaListenerConfig();

        setConcurrency(config, 3);

        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.getListener().setPollTimeout(null);

        long defaultPollTimeout =
                new ConcurrentKafkaListenerContainerFactory<>()
                        .getContainerProperties()
                        .getPollTimeout();

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.budgetingKafkaListenerContainerFactory(
                        consumerFactory(),
                        errorHandler(),
                        kafkaProperties
                );

        assertNotNull(factory);
        assertEquals(defaultPollTimeout, factory.getContainerProperties().getPollTimeout());
    }

    @Test
    void shouldHaveConfigurationAnnotation() {
        Configuration annotation =
                KafkaListenerConfig.class.getAnnotation(Configuration.class);

        assertNotNull(annotation);
    }

    @Test
    void shouldHaveBeanAnnotationOnBudgetingKafkaListenerContainerFactoryMethod()
            throws Exception {
        Method method = KafkaListenerConfig.class.getDeclaredMethod(
                "budgetingKafkaListenerContainerFactory",
                ConsumerFactory.class,
                DefaultErrorHandler.class,
                KafkaProperties.class
        );

        Bean bean = method.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertArrayEquals(
                new String[]{"budgetingKafkaListenerContainerFactory"},
                bean.name()
        );
        assertEquals(
                ConcurrentKafkaListenerContainerFactory.class,
                method.getReturnType()
        );
    }

    @Test
    void shouldHaveValueAnnotationOnConcurrencyField() throws Exception {
        Field field = KafkaListenerConfig.class.getDeclaredField("concurrency");

        Value value = field.getAnnotation(Value.class);

        assertNotNull(value);
        assertEquals(
                "${ecofy.budgeting.kafka.listener.concurrency:3}",
                value.value()
        );
    }

    private static void setConcurrency(
            KafkaListenerConfig config,
            int concurrency
    ) throws Exception {
        Field field = KafkaListenerConfig.class.getDeclaredField("concurrency");
        field.setAccessible(true);
        field.setInt(config, concurrency);
    }

    private static Object readField(
            Object target,
            String fieldName
    ) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object readFieldInHierarchy(
            Object target,
            String fieldName
    ) throws Exception {
        Class<?> current = target.getClass();

        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        throw new AssertionError(
                "Field not found in hierarchy: " + fieldName
        );
    }

    @SuppressWarnings("unchecked")
    private static ConsumerFactory<Object, Object> consumerFactory() {
        return mock(ConsumerFactory.class);
    }

    private static DefaultErrorHandler errorHandler() {
        return mock(DefaultErrorHandler.class);
    }
}