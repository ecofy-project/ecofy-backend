package br.com.ecofy.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Configura e expõe um ObjectMapper padrão (JavaTime + datas ISO-8601 + tolerância a campos desconhecidos).
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Registra suporte a tipos de data/hora do Java (java.time.*).
        mapper.registerModule(new JavaTimeModule());

        // Serializa datas como texto ISO-8601 em vez de timestamps numéricos.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Ignora propriedades desconhecidas no JSON para evitar falhas de desserialização.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}
