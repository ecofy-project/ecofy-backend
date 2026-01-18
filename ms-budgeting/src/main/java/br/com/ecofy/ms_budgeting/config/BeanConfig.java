package br.com.ecofy.ms_budgeting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BeanConfig {

    // Expõe um Clock configurado na timezone padrão do sistema para uso e testes determinísticos via injeção.
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

}
