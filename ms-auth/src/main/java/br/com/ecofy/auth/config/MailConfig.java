package br.com.ecofy.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Configura as propriedades utilizadas no envio de e-mails.
@Configuration
@EnableConfigurationProperties(MailConfig.EcofyMailProperties.class)
public class MailConfig {

    // Representa falhas relacionadas à configuração de e-mail.
    public static class MailConfigException extends RuntimeException {

        public MailConfigException(String message) {
            super(message);
        }
    }

    // Agrupa as propriedades de remetente, frontend e templates de e-mail.
    @ConfigurationProperties(prefix = "ecofy.mail")
    public static class EcofyMailProperties {

        private String from = "no-reply@ecofy.com";

        private String frontendBaseUrl = "https://app.ecofy.com";

        private String templatesBasePath = "classpath:/mail-templates";

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getFrontendBaseUrl() {
            return frontendBaseUrl;
        }

        public void setFrontendBaseUrl(String frontendBaseUrl) {
            this.frontendBaseUrl = frontendBaseUrl;
        }

        public String getTemplatesBasePath() {
            return templatesBasePath;
        }

        public void setTemplatesBasePath(String templatesBasePath) {
            this.templatesBasePath = templatesBasePath;
        }
    }
}
