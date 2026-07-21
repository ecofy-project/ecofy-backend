package br.com.ecofy.auth.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configura o relógio utilizado pelas operações temporais da aplicação.
@Configuration
public class ClockConfig {

    // Registra um relógio UTC quando nenhuma implementação estiver disponível.
    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
