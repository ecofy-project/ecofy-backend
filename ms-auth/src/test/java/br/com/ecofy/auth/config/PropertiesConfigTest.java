package br.com.ecofy.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Testes unitários da configuração de propriedades")
class PropertiesConfigTest {

    @Test
    @DisplayName("Deve criar a configuração de propriedades corretamente")
    void constructor_novaInstancia_deveCriarConfiguracao() {
        // Arrange
        // Act
        PropertiesConfig config = new PropertiesConfig();

        // Assert
        assertNotNull(config);
    }

    @Test
    @DisplayName("Deve registrar todas as classes de propriedades utilizadas pelo serviço")
    void annotations_configuracaoDeclarada_deveRegistrarTodasAsPropriedades() {
        // Arrange
        Class<PropertiesConfig> configClass = PropertiesConfig.class;

        // Act
        Configuration configuration =
                configClass.getAnnotation(Configuration.class);

        EnableConfigurationProperties enableProperties =
                configClass.getAnnotation(
                        EnableConfigurationProperties.class
                );

        // Assert
        assertAll(
                () -> assertNotNull(configuration),
                () -> assertNotNull(enableProperties),
                () -> assertArrayEquals(
                        new Class<?>[]{
                                UsersMsProperties.class,
                                RateLimitProperties.class,
                                BruteForceProperties.class,
                                KeysProperties.class
                        },
                        enableProperties.value()
                )
        );
    }
}
