package br.com.ecofy.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesConfigTest {

    @Test
    void shouldInstantiatePropertiesConfig() {
        PropertiesConfig config = new PropertiesConfig();

        assertNotNull(config);
    }

    @Test
    void shouldBeAnnotatedWithConfiguration() {
        Configuration annotation = PropertiesConfig.class.getAnnotation(Configuration.class);

        assertNotNull(annotation);
    }

    @Test
    void shouldEnableUsersMsProperties() {
        EnableConfigurationProperties annotation =
                PropertiesConfig.class.getAnnotation(EnableConfigurationProperties.class);

        assertNotNull(annotation);
        assertArrayEquals(
                new Class<?>[]{UsersMsProperties.class},
                annotation.value()
        );
    }
}