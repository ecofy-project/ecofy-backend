package br.com.ecofy.gateway.api_gateway.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura, a partir de {@link ResilienceProperties}, a política padrão de
 * circuit breaker + time limiter aplicada a todas as instâncias nomeadas usadas
 * pelas rotas versionadas (ECO-21 §8.4).
 *
 * Configuração 100% externa (typed properties); nenhum valor é codificado na
 * classe. Cada rota referencia um circuit breaker próprio por nome
 * ({@code cb-<serviço>}), obtendo estado e métricas isolados por serviço, com a
 * mesma política padrão.
 */
@Configuration
public class GatewayResilienceConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer(
            ResilienceProperties properties) {

        ResilienceProperties.CircuitBreaker cb = properties.getCircuitBreaker();

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(cb.getSlidingWindowSize())
                .minimumNumberOfCalls(cb.getMinimumNumberOfCalls())
                .failureRateThreshold(cb.getFailureRateThreshold())
                .waitDurationInOpenState(cb.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(cb.getPermittedNumberOfCallsInHalfOpenState())
                // 4xx funcionais chegam como resposta normal (não exceção) e não são
                // contabilizados; apenas exceções de transporte/timeout contam como falha.
                .build();

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(cb.getTimeLimiterTimeout())
                .build();

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(circuitBreakerConfig)
                .timeLimiterConfig(timeLimiterConfig)
                .build());
    }
}
