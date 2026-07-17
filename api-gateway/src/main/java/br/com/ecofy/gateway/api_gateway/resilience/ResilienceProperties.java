package br.com.ecofy.gateway.api_gateway.resilience;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuração tipada do circuit breaker e time limiter do gateway (ECO-21),
 * prefixo {@code ecofy.gateway.resilience}.
 *
 * Aplica-se como configuração padrão a todas as instâncias nomeadas de circuit
 * breaker das rotas versionadas (uma por serviço: {@code cb-auth}, {@code cb-users}...).
 * O time limiter é mantido acima do {@code response-timeout} das rotas para que o
 * timeout por rota (Netty) seja o mecanismo efetivo de expiração, evitando dois
 * relógios competindo.
 */
@Validated
@ConfigurationProperties(prefix = "ecofy.gateway.resilience")
public class ResilienceProperties {

    private final CircuitBreaker circuitBreaker = new CircuitBreaker();

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public static class CircuitBreaker {

        @Min(1)
        private int slidingWindowSize = 20;

        @Min(1)
        private int minimumNumberOfCalls = 10;

        @Min(1)
        @Max(100)
        private float failureRateThreshold = 50f;

        private Duration waitDurationInOpenState = Duration.ofSeconds(10);

        @Min(1)
        private int permittedNumberOfCallsInHalfOpenState = 3;

        /** Deve ser maior que o maior {@code response-timeout} de rota. */
        private Duration timeLimiterTimeout = Duration.ofSeconds(10);

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
        }

        public float getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(float failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public Duration getWaitDurationInOpenState() {
            return waitDurationInOpenState;
        }

        public void setWaitDurationInOpenState(Duration waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public int getPermittedNumberOfCallsInHalfOpenState() {
            return permittedNumberOfCallsInHalfOpenState;
        }

        public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) {
            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
        }

        public Duration getTimeLimiterTimeout() {
            return timeLimiterTimeout;
        }

        public void setTimeLimiterTimeout(Duration timeLimiterTimeout) {
            this.timeLimiterTimeout = timeLimiterTimeout;
        }
    }
}
