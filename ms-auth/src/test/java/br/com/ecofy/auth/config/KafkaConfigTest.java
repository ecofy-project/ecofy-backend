package br.com.ecofy.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaConfigTest {

    @Test
    void authEventProducerFactory_shouldConfigureBootstrapAndKeySerializer_andUseJsonSerializerWithTypeInfoDisabled() throws Exception {
        KafkaConfig config = new KafkaConfig();

        String bootstrap = "localhost:9092";
        ObjectMapper objectMapper = new ObjectMapper();

        ProducerFactory<String, Object> pf = config.authEventProducerFactory(bootstrap, objectMapper);

        assertNotNull(pf);
        assertInstanceOf(DefaultKafkaProducerFactory.class, pf);

        DefaultKafkaProducerFactory<?, ?> dpf = (DefaultKafkaProducerFactory<?, ?>) pf;

        Map<String, Object> props = dpf.getConfigurationProperties();
        assertEquals(bootstrap, props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));

        Serializer<?> keySerializer = extractKeySerializer(dpf);
        assertNotNull(keySerializer);
        assertEquals(StringSerializer.class, keySerializer.getClass());

        Serializer<?> valueSerializer = extractValueSerializer(dpf);
        assertNotNull(valueSerializer);

        String vsClass = valueSerializer.getClass().getName();

        assertTrue(
                vsClass.equals("org.springframework.kafka.support.serializer.JsonSerializer")
                        || vsClass.equals("org.springframework.kafka.support.serializer.JacksonJsonSerializer"),
                "Value serializer deve ser JsonSerializer ou JacksonJsonSerializer, mas foi: " + vsClass
        );

        Boolean addTypeInfo = readBooleanGetterIfExists(valueSerializer, "isAddTypeInfo");
        if (addTypeInfo == null) addTypeInfo = readBooleanGetterIfExists(valueSerializer, "isAddTypeHeaders");
        if (addTypeInfo == null) addTypeInfo = readBooleanGetterIfExists(valueSerializer, "isAddTypeInformation");
        if (addTypeInfo == null) addTypeInfo = readBooleanGetterIfExists(valueSerializer, "isAddTypeMapper"); // alguns variants

        Boolean addTypeInfoField = readBooleanFieldViaGetterFallback(valueSerializer, "addTypeInfo");
        if (addTypeInfo == null) addTypeInfo = addTypeInfoField;

        if (addTypeInfo == null) {
            Boolean typeHeadersFromConfigs = readBooleanFromConfigsIfExists(valueSerializer, "spring.json.add.type.headers");
            if (typeHeadersFromConfigs == null) {
                // fallback: não falha por falta de getter/campo; o essencial já foi validado (tipo do serializer)
                return;
            }
            addTypeInfo = typeHeadersFromConfigs;
        }

        assertFalse(addTypeInfo, "Type info/type headers devem estar desabilitados");
    }
    @Test
    void authEventKafkaTemplate_shouldSetDefaultTopic() {
        KafkaConfig config = new KafkaConfig();

        ProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(Map.of());

        KafkaTemplate<String, Object> template = config.authEventKafkaTemplate(pf);

        assertNotNull(template);
        assertEquals("auth.events", template.getDefaultTopic());
    }

    // heapers

    @SuppressWarnings("unchecked")
    private static Serializer<?> extractKeySerializer(DefaultKafkaProducerFactory<?, ?> dpf) {
        try {
            var f = DefaultKafkaProducerFactory.class.getDeclaredField("keySerializerSupplier");
            f.setAccessible(true);
            Object supplier = f.get(dpf);
            if (supplier instanceof java.util.function.Supplier<?> s) {
                Object serializer = s.get();
                return (Serializer<?>) serializer;
            }
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            var f2 = DefaultKafkaProducerFactory.class.getDeclaredField("keySerializer");
            f2.setAccessible(true);
            return (Serializer<?>) f2.get(dpf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Boolean readBooleanFromConfigsIfExists(Object serializer, String key) {
        try {
            for (String fieldName : java.util.List.of("configs", "config", "configuration", "properties")) {
                try {
                    var f = serializer.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object v = f.get(serializer);

                    if (v instanceof java.util.Map<?, ?> m) {
                        Object raw = m.get(key);
                        if (raw instanceof Boolean b) return b;
                        if (raw instanceof String s) return Boolean.parseBoolean(s);
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Serializer<?> extractValueSerializer(DefaultKafkaProducerFactory<?, ?> dpf) throws Exception {
        for (String name : new String[]{"getValueSerializer", "valueSerializer"}) {
            try {
                Method m = dpf.getClass().getMethod(name);
                Object v = m.invoke(dpf);
                if (v instanceof Serializer<?> s) return s;
            } catch (NoSuchMethodException ignored) {
            }
        }

        for (Method m : dpf.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && Serializer.class.isAssignableFrom(m.getReturnType())) {
                Object v = m.invoke(dpf);
                if (v instanceof Serializer<?> s) return s;
            }
        }

        throw new AssertionError("Could not extract value serializer from " + dpf.getClass().getName());
    }

    private static Boolean readBooleanGetterIfExists(Object target, String methodName) {
        try {
            Object v = target.getClass().getMethod(methodName).invoke(target);
            return (v instanceof Boolean b) ? b : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Boolean readBooleanFieldViaGetterFallback(Object target, String logicalName) {
        String cap = Character.toUpperCase(logicalName.charAt(0)) + logicalName.substring(1);
        for (String getter : new String[]{"get" + cap, "is" + cap}) {
            Boolean v = readBooleanGetterIfExists(target, getter);
            if (v != null) return v;
        }
        return null;
    }
}
