package br.com.ecofy.ms_categorization.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configura um ObjectMapper Jackson 2 explícito, exigido pela serialização do Kafka e da Outbox.
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Suporte a java.time.* (Instant/LocalDate do envelope).
        mapper.registerModule(new JavaTimeModule());

        // Datas como ISO-8601, não timestamps numéricos.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Campo novo adicionado pelo produtor não derruba o consumer (evolução compatível §14.3).
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}
