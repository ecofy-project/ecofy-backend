package br.com.ecofy.ms_insights.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class TimeConfig {

    // Expõe um Clock em UTC para padronizar timestamps e facilitar testes determinísticos em toda a aplicação.
    @Bean
    public Clock clock() {
        // UTC para consistência com: spring.jackson.time-zone=UTC e hibernate.jdbc.time_zone=UTC
        return Clock.system(ZoneOffset.UTC);
    }

}
