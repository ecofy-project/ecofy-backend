package br.com.ecofy.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String AUTH_EVENTS_TOPIC = "auth.events";

    @Bean
    public ProducerFactory<String, Object> authEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            ObjectMapper jsonMapper
    ) {
        Map<String, Object> props = new HashMap<>();

        // Config básica do producer
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Serializer de valor (Spring Kafka 4 / Jackson 3)
        JacksonJsonSerializer<Object> valueSerializer = new JacksonJsonSerializer<>(jsonMapper);
        valueSerializer.setAddTypeInfo(false); // equivalente ao setAddTypeInfo(false) do antigo JsonSerializer

        // Forma mais direta: injeta os serializers no factory
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                valueSerializer
        );
    }

    @Bean
    public KafkaTemplate<String, Object> authEventKafkaTemplate(
            ProducerFactory<String, Object> authEventProducerFactory
    ) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(authEventProducerFactory);
        template.setDefaultTopic(AUTH_EVENTS_TOPIC);
        return template;
    }
}
