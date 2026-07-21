package br.com.ecofy.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

// Configura os componentes responsáveis pela publicação dos eventos no Kafka.
@Configuration
public class KafkaConfig {

    public static final String AUTH_EVENTS_TOPIC = "auth.events";

    // Registra a fábrica de produtores com serialização JSON sem headers de tipo.
    @Bean
    public ProducerFactory<String, Object> authEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> props = new HashMap<>();

        props.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        JacksonJsonSerializer<Object> valueSerializer =
                new JacksonJsonSerializer<>();

        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                valueSerializer
        );
    }

    // Registra o template utilizado para publicar eventos de autenticação.
    @Bean
    public KafkaTemplate<String, Object> authEventKafkaTemplate(
            ProducerFactory<String, Object> authEventProducerFactory
    ) {
        KafkaTemplate<String, Object> template =
                new KafkaTemplate<>(authEventProducerFactory);

        template.setDefaultTopic(AUTH_EVENTS_TOPIC);

        return template;
    }
}
