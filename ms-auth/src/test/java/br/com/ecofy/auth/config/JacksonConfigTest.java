package br.com.ecofy.auth.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Testes unitários da configuração do Jackson")
class JacksonConfigTest {

    @Test
    @DisplayName("Deve criar ObjectMapper com as configurações esperadas")
    void objectMapper_configuracaoCriada_deveRetornarMapperConfigurado() {
        // Arrange
        JacksonConfig config = new JacksonConfig();

        // Act
        ObjectMapper result = config.objectMapper();

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertFalse(
                        result.isEnabled(
                                SerializationFeature
                                        .WRITE_DATES_AS_TIMESTAMPS
                        )
                ),
                () -> assertFalse(
                        result.isEnabled(
                                DeserializationFeature
                                        .FAIL_ON_UNKNOWN_PROPERTIES
                        )
                )
        );
    }

    @Test
    @DisplayName("Deve serializar Instant utilizando o formato textual ISO-8601")
    void objectMapper_instantValido_deveSerializarComoTextoIso()
            throws Exception {
        // Arrange
        ObjectMapper mapper = new JacksonConfig().objectMapper();
        Instant instant = Instant.parse(
                "2026-07-20T12:30:45Z"
        );

        // Act
        String result = mapper.writeValueAsString(
                Map.of("instant", instant)
        );

        // Assert
        assertEquals(
                "{\"instant\":\"2026-07-20T12:30:45Z\"}",
                result
        );
    }

    @Test
    @DisplayName("Deve desserializar LocalDate utilizando o módulo de datas Java")
    void objectMapper_dataIsoValida_deveDesserializarLocalDate()
            throws Exception {
        // Arrange
        ObjectMapper mapper = new JacksonConfig().objectMapper();
        String json = "\"2026-07-20\"";

        // Act
        LocalDate result = mapper.readValue(
                json,
                LocalDate.class
        );

        // Assert
        assertEquals(
                LocalDate.of(2026, 7, 20),
                result
        );
    }

    @Test
    @DisplayName("Deve ignorar campos desconhecidos durante a desserialização")
    void objectMapper_jsonComCampoDesconhecido_deveDesserializarSemErro()
            throws Exception {
        // Arrange
        ObjectMapper mapper = new JacksonConfig().objectMapper();
        String json = """
                {
                  "name": "EcoFy",
                  "unknownField": "valor ignorado"
                }
                """;

        // Act
        TestPayload result = mapper.readValue(
                json,
                TestPayload.class
        );

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals("EcoFy", result.name)
        );
    }

    private static class TestPayload {

        public String name;
    }
}
