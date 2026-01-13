package br.com.ecofy.ms_users.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UsersProperties.class)
public class PropertiesConfig {
    // IMPORTANTE:
    // Não crie @Bean UsersProperties aqui. O EnableConfigurationProperties já registra o bean.
}
