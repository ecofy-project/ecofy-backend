package br.com.ecofy.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Testes unitários da configuração de e-mail")
class MailConfigTest {

    @Test
    @DisplayName("Deve criar a configuração de e-mail corretamente")
    void constructor_novaInstancia_deveCriarConfiguracao() {
        // Arrange
        // Act
        MailConfig config = new MailConfig();

        // Assert
        assertNotNull(config);
    }

    @Test
    @DisplayName("Deve inicializar as propriedades de e-mail com os valores padrão")
    void constructor_novasPropriedades_deveRetornarValoresPadrao() {
        // Arrange
        MailConfig.EcofyMailProperties properties =
                new MailConfig.EcofyMailProperties();

        // Act
        String from = properties.getFrom();
        String frontendBaseUrl = properties.getFrontendBaseUrl();
        String templatesBasePath = properties.getTemplatesBasePath();

        // Assert
        assertAll(
                () -> assertEquals(
                        "no-reply@ecofy.com",
                        from
                ),
                () -> assertEquals(
                        "https://app.ecofy.com",
                        frontendBaseUrl
                ),
                () -> assertEquals(
                        "classpath:/mail-templates",
                        templatesBasePath
                )
        );
    }

    @Test
    @DisplayName("Deve atualizar todas as propriedades de e-mail com os valores informados")
    void setters_valoresValidos_deveAtualizarTodasAsPropriedades() {
        // Arrange
        MailConfig.EcofyMailProperties properties =
                new MailConfig.EcofyMailProperties();

        String from = "contato@ecofy.com";
        String frontendBaseUrl = "https://frontend.ecofy.com";
        String templatesBasePath = "classpath:/templates/mail";

        // Act
        properties.setFrom(from);
        properties.setFrontendBaseUrl(frontendBaseUrl);
        properties.setTemplatesBasePath(templatesBasePath);

        // Assert
        assertAll(
                () -> assertEquals(from, properties.getFrom()),
                () -> assertEquals(
                        frontendBaseUrl,
                        properties.getFrontendBaseUrl()
                ),
                () -> assertEquals(
                        templatesBasePath,
                        properties.getTemplatesBasePath()
                )
        );
    }

    @Test
    @DisplayName("Deve armazenar valores nulos nas propriedades sem validação direta")
    void setters_valoresNulos_deveArmazenarValoresNulos() {
        // Arrange
        MailConfig.EcofyMailProperties properties =
                new MailConfig.EcofyMailProperties();

        // Act
        properties.setFrom(null);
        properties.setFrontendBaseUrl(null);
        properties.setTemplatesBasePath(null);

        // Assert
        assertAll(
                () -> assertNull(properties.getFrom()),
                () -> assertNull(properties.getFrontendBaseUrl()),
                () -> assertNull(properties.getTemplatesBasePath())
        );
    }

    @Test
    @DisplayName("Deve armazenar valores vazios e em branco sem validação direta")
    void setters_valoresVaziosEEmBranco_deveArmazenarValoresInformados() {
        // Arrange
        MailConfig.EcofyMailProperties properties =
                new MailConfig.EcofyMailProperties();

        // Act
        properties.setFrom("");
        properties.setFrontendBaseUrl("   ");
        properties.setTemplatesBasePath("");

        // Assert
        assertAll(
                () -> assertEquals("", properties.getFrom()),
                () -> assertEquals(
                        "   ",
                        properties.getFrontendBaseUrl()
                ),
                () -> assertEquals(
                        "",
                        properties.getTemplatesBasePath()
                )
        );
    }

    @Test
    @DisplayName("Deve criar exceção de configuração preservando a mensagem informada")
    void constructor_mensagemValida_deveCriarExcecaoComMensagem() {
        // Arrange
        String message = "Configuração de e-mail inválida";

        // Act
        MailConfig.MailConfigException exception =
                new MailConfig.MailConfigException(message);

        // Assert
        assertAll(
                () -> assertNotNull(exception),
                () -> assertEquals(message, exception.getMessage())
        );
    }

    @Test
    @DisplayName("Deve criar exceção de configuração com mensagem nula")
    void constructor_mensagemNula_deveCriarExcecaoComMensagemNula() {
        // Arrange
        String message = null;

        // Act
        MailConfig.MailConfigException exception =
                new MailConfig.MailConfigException(message);

        // Assert
        assertAll(
                () -> assertNotNull(exception),
                () -> assertNull(exception.getMessage())
        );
    }
}
