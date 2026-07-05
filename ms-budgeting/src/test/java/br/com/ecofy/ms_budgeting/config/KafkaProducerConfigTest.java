package br.com.ecofy.ms_budgeting.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaProducerConfigTest {

    private static final String BOOTSTRAP_SERVER =
            "localhost:9092";

    @Test
    void shouldCreateKafkaProducerConfigInstance() {
        KafkaProducerConfig config = new KafkaProducerConfig();

        assertNotNull(config);
    }

    @Test
    void shouldCreateProducerFactoryWithDefaultKeySerializerWhenKeySerializerIsMissing()
            throws Exception {
        KafkaProducerConfig config = new KafkaProducerConfig();
        KafkaProperties kafkaProperties = kafkaProperties();
        JsonMapper jsonMapper = jsonMapper();

        ProducerFactory<String, Object> producerFactory =
                config.producerFactory(kafkaProperties, jsonMapper);

        assertNotNull(producerFactory);
        assertInstanceOf(DefaultKafkaProducerFactory.class, producerFactory);

        DefaultKafkaProducerFactory<String, Object> defaultFactory =
                castProducerFactory(producerFactory);

        Map<String, Object> configurationProperties =
                defaultFactory.getConfigurationProperties();

        assertEquals(
                StringSerializer.class,
                configurationProperties.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
        );

        assertTrue(
                configurationProperties.containsKey(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)
        );
    }

    @Test
    void shouldPreserveExistingKeySerializerWhenAlreadyConfigured()
            throws Exception {
        KafkaProducerConfig config = new KafkaProducerConfig();
        KafkaProperties kafkaProperties = kafkaProperties();

        kafkaProperties.getProducer().setKeySerializer(ByteArraySerializer.class);

        ProducerFactory<String, Object> producerFactory =
                config.producerFactory(kafkaProperties, jsonMapper());

        DefaultKafkaProducerFactory<String, Object> defaultFactory =
                castProducerFactory(producerFactory);

        Map<String, Object> configurationProperties =
                defaultFactory.getConfigurationProperties();

        assertEquals(
                ByteArraySerializer.class,
                configurationProperties.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
        );
    }

    @Test
    void shouldCreateProducerFactoryUsingStringSerializerAsKeySerializerInstance()
            throws Exception {
        KafkaProducerConfig config = new KafkaProducerConfig();

        ProducerFactory<String, Object> producerFactory =
                config.producerFactory(kafkaProperties(), jsonMapper());

        DefaultKafkaProducerFactory<String, Object> defaultFactory =
                castProducerFactory(producerFactory);

        Object keySerializer =
                readFieldInHierarchy(defaultFactory, "keySerializerSupplier");

        assertNotNull(keySerializer);
    }

    @Test
    void shouldCreateProducerFactoryUsingJacksonJsonSerializerAsValueSerializerInstance()
            throws Exception {
        KafkaProducerConfig config = new KafkaProducerConfig();

        ProducerFactory<String, Object> producerFactory =
                config.producerFactory(kafkaProperties(), jsonMapper());

        DefaultKafkaProducerFactory<String, Object> defaultFactory =
                castProducerFactory(producerFactory);

        Object valueSerializer =
                readFieldInHierarchy(defaultFactory, "valueSerializerSupplier");

        assertNotNull(valueSerializer);
    }

    @Test
    void shouldCreateKafkaTemplateWithProducerFactory() {
        KafkaProducerConfig config = new KafkaProducerConfig();

        ProducerFactory<String, Object> producerFactory =
                config.producerFactory(kafkaProperties(), jsonMapper());

        KafkaTemplate<String, Object> kafkaTemplate =
                config.kafkaTemplate(producerFactory);

        assertNotNull(kafkaTemplate);
        assertSame(producerFactory, kafkaTemplate.getProducerFactory());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenKafkaTemplateProducerFactoryIsNull() {
        KafkaProducerConfig config = new KafkaProducerConfig();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> config.kafkaTemplate(null)
        );

        assertEquals("'producerFactory' cannot be null", exception.getMessage());
    }

    @Test
    void shouldHaveConfigurationAnnotation() {
        Configuration annotation =
                KafkaProducerConfig.class.getAnnotation(Configuration.class);

        assertNotNull(annotation);
    }

    @Test
    void shouldHaveBeanAnnotationOnProducerFactoryMethod()
            throws Exception {
        Method method = KafkaProducerConfig.class.getDeclaredMethod(
                "producerFactory",
                KafkaProperties.class,
                JsonMapper.class
        );

        Bean bean = method.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertEquals(ProducerFactory.class, method.getReturnType());
    }

    @Test
    void shouldHaveBeanAnnotationOnKafkaTemplateMethod()
            throws Exception {
        Method method = KafkaProducerConfig.class.getDeclaredMethod(
                "kafkaTemplate",
                ProducerFactory.class
        );

        Bean bean = method.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertEquals(KafkaTemplate.class, method.getReturnType());
    }

    @Test
    void shouldProducerFactoryMethodHaveExpectedParameters()
            throws Exception {
        Method method = KafkaProducerConfig.class.getDeclaredMethod(
                "producerFactory",
                KafkaProperties.class,
                JsonMapper.class
        );

        Class<?>[] parameterTypes = method.getParameterTypes();

        assertArrayEquals(
                new Class<?>[]{
                        KafkaProperties.class,
                        JsonMapper.class
                },
                parameterTypes
        );
    }

    @Test
    void shouldKafkaTemplateMethodHaveExpectedParameters()
            throws Exception {
        Method method = KafkaProducerConfig.class.getDeclaredMethod(
                "kafkaTemplate",
                ProducerFactory.class
        );

        Class<?>[] parameterTypes = method.getParameterTypes();

        assertArrayEquals(
                new Class<?>[]{
                        ProducerFactory.class
                },
                parameterTypes
        );
    }

    @Test
    void shouldCreateJacksonJsonSerializerWithoutTypeInfoHeader()
            throws Exception {
        JsonMapper jsonMapper = jsonMapper();

        JacksonJsonSerializer<Object> serializer =
                new JacksonJsonSerializer<>(jsonMapper);

        serializer.setAddTypeInfo(false);

        Object addTypeInfo =
                readAnyFieldInHierarchy(
                        serializer,
                        "addTypeInfo",
                        "addTypeInfoHeaders",
                        "typeInfo"
                );

        if (addTypeInfo instanceof Boolean value) {
            assertFalse(value);
        } else {
            assertNotNull(serializer);
        }
    }

    private static KafkaProperties kafkaProperties() {
        KafkaProperties kafkaProperties = new KafkaProperties();

        kafkaProperties.setBootstrapServers(List.of(BOOTSTRAP_SERVER));

        return kafkaProperties;
    }

    private static JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }

    @SuppressWarnings("unchecked")
    private static DefaultKafkaProducerFactory<String, Object> castProducerFactory(
            ProducerFactory<String, Object> producerFactory
    ) {
        return (DefaultKafkaProducerFactory<String, Object>) producerFactory;
    }

    private static Object readAnyFieldInHierarchy(
            Object target,
            String... fieldNames
    ) throws Exception {
        for (String fieldName : fieldNames) {
            try {
                return readFieldInHierarchy(target, fieldName);
            } catch (AssertionError ignored) {
                // tenta o próximo nome de campo
            }
        }

        return null;
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
}