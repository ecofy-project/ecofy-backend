package br.com.ecofy.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Testes unitários da configuração do Kafka")
class KafkaConfigTest {

    private static final String BOOTSTRAP_SERVERS =
            "localhost:9092";

    @Test
    @DisplayName("Deve criar a fábrica de produtores com os servidores e serializadores configurados")
    void authEventProducerFactory_configuracaoValida_deveRetornarFabricaConfigurada() {
        // Arrange
        KafkaConfig config = new KafkaConfig();
        ObjectMapper objectMapper = new ObjectMapper();

        // Act
        ProducerFactory<String, Object> result =
                config.authEventProducerFactory(
                        BOOTSTRAP_SERVERS,
                        objectMapper
                );

        // Assert
        DefaultKafkaProducerFactory<String, Object> factory =
                assertInstanceOf(
                        DefaultKafkaProducerFactory.class,
                        result
                );

        Serializer<String> keySerializer =
                factory.getKeySerializerSupplier().get();
        Serializer<Object> valueSerializer =
                factory.getValueSerializerSupplier().get();

        assertAll(
                () -> assertEquals(
                        BOOTSTRAP_SERVERS,
                        factory.getConfigurationProperties().get(
                                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
                        )
                ),
                () -> assertInstanceOf(
                        StringSerializer.class,
                        keySerializer
                ),
                () -> assertInstanceOf(
                        JacksonJsonSerializer.class,
                        valueSerializer
                )
        );
    }

    @Test
    @DisplayName("Deve serializar a chave como texto e o valor como JSON sem adicionar headers de tipo")
    void authEventProducerFactory_eventoValido_deveSerializarSemHeadersDeTipo() {
        // Arrange
        KafkaConfig config = new KafkaConfig();

        DefaultKafkaProducerFactory<String, Object> factory =
                (DefaultKafkaProducerFactory<String, Object>)
                        config.authEventProducerFactory(
                                BOOTSTRAP_SERVERS,
                                new ObjectMapper()
                        );

        Serializer<String> keySerializer =
                factory.getKeySerializerSupplier().get();
        Serializer<Object> valueSerializer =
                factory.getValueSerializerSupplier().get();

        RecordHeaders headers = new RecordHeaders();
        Map<String, Object> event =
                Map.of("eventType", "auth.user.registered");

        // Act
        byte[] serializedKey = keySerializer.serialize(
                KafkaConfig.AUTH_EVENTS_TOPIC,
                "user-id"
        );

        byte[] serializedValue = valueSerializer.serialize(
                KafkaConfig.AUTH_EVENTS_TOPIC,
                headers,
                event
        );

        // Assert
        assertAll(
                () -> assertArrayEquals(
                        "user-id".getBytes(StandardCharsets.UTF_8),
                        serializedKey
                ),
                () -> assertEquals(
                        "{\"eventType\":\"auth.user.registered\"}",
                        new String(
                                serializedValue,
                                StandardCharsets.UTF_8
                        )
                ),
                () -> assertFalse(headers.iterator().hasNext())
        );
    }

    @Test
    @DisplayName("Deve criar a fábrica mesmo quando o ObjectMapper informado for nulo")
    void authEventProducerFactory_objectMapperNulo_deveRetornarFabricaConfigurada() {
        // Arrange
        KafkaConfig config = new KafkaConfig();

        // Act
        ProducerFactory<String, Object> result =
                config.authEventProducerFactory(
                        BOOTSTRAP_SERVERS,
                        null
                );

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Deve lançar exceção quando os servidores Kafka forem nulos")
    void authEventProducerFactory_bootstrapServersNulos_deveLancarNullPointerException() {
        // Arrange
        KafkaConfig config = new KafkaConfig();

        // Act
        // Assert
        assertThrows(
                NullPointerException.class,
                () -> config.authEventProducerFactory(
                        null,
                        new ObjectMapper()
                )
        );
    }

    @Test
    @DisplayName("Deve criar o template Kafka com a fábrica e o tópico padrão de autenticação")
    void authEventKafkaTemplate_fabricaValida_deveRetornarTemplateConfigurado() {
        // Arrange
        KafkaConfig config = new KafkaConfig();

        ProducerFactory<String, Object> producerFactory =
                config.authEventProducerFactory(
                        BOOTSTRAP_SERVERS,
                        new ObjectMapper()
                );

        // Act
        KafkaTemplate<String, Object> result =
                config.authEventKafkaTemplate(producerFactory);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertSame(
                        producerFactory,
                        result.getProducerFactory()
                ),
                () -> assertEquals(
                        KafkaConfig.AUTH_EVENTS_TOPIC,
                        result.getDefaultTopic()
                )
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando a fábrica de produtores for nula")
    void authEventKafkaTemplate_fabricaNula_deveLancarIllegalArgumentException() {
        // Arrange
        KafkaConfig config = new KafkaConfig();

        // Act
        // Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> config.authEventKafkaTemplate(null)
        );
    }
}
