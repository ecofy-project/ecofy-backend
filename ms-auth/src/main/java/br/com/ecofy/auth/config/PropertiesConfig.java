package br.com.ecofy.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Registra as propriedades tipadas utilizadas pelas configurações do serviço.
@Configuration
@EnableConfigurationProperties({
        UsersMsProperties.class,
        RateLimitProperties.class,
        BruteForceProperties.class,
        KeysProperties.class
})
public class PropertiesConfig {

}
