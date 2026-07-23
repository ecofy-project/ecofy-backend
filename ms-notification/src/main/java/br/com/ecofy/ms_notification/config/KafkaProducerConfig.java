package br.com.ecofy.ms_notification.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;
    private final JsonMapper jsonMapper;

    // Cria o ProducerFactory com key String e value JSON (JacksonJsonSerializer) usando JsonMapper (Jackson 3).
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

        // Define serializer da key como String quando não houver configuração explícita.
        props.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Instancia serializer JSON para o value, desabilitando type info/headers para evitar acoplamento e riscos.
        JacksonJsonSerializer<Object> valueSerializer = new JacksonJsonSerializer<>(jsonMapper);
        valueSerializer.setAddTypeInfo(false); // não adiciona type headers/type info no payload.

        // Habilita confiabilidade do producer (acks=all) e idempotência para evitar duplicatas em retries.
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        props.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Constrói o ProducerFactory com serializers explícitos (key String, value JSON) para consistência.
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                valueSerializer
        );
    }

    // Cria o KafkaTemplate usado pelos adapters para publicar eventos no Kafka com o ProducerFactory configurado.
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // Producer/Template DEDICADO para a DLT (ECO-02): byte[]/byte[] republica os BYTES ORIGINAIS
    // da mensagem verbatim (inclusive quando a falha foi de desserialização), sem re-serializar.
    @Bean
    public ProducerFactory<byte[], byte[]> dltProducerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props, new ByteArraySerializer(), new ByteArraySerializer());
    }

    @Bean
    public KafkaTemplate<byte[], byte[]> dltKafkaTemplate(ProducerFactory<byte[], byte[]> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    // Template String/String para o publisher da Outbox (ECO-32): o payload já é o envelope JSON
    // serializado; StringSerializer publica os bytes verbatim (sem dupla codificação).
    @Bean
    public ProducerFactory<String, String> outboxStringProducerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        props.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer());
    }

    @Bean
    public KafkaTemplate<String, String> notificationOutboxKafkaTemplate(
            ProducerFactory<String, String> outboxStringProducerFactory) {
        return new KafkaTemplate<>(outboxStringProducerFactory);
    }

}
