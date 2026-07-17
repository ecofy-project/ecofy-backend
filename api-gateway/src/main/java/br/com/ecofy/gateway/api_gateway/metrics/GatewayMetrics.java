package br.com.ecofy.gateway.api_gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Métricas técnicas customizadas do gateway (ECO-16), complementares às métricas
 * nativas ({@code spring.cloud.gateway.requests}) e às do Resilience4j.
 *
 * Cardinalidade controlada: nenhuma tag carrega correlation ID, user ID, token,
 * path com identificadores dinâmicos ou mensagens de exceção. A única tag usada
 * ({@code type} em fallbacks) provém de um enum fechado.
 */
@Component
public class GatewayMetrics {

    private final Counter correlationIdMissing;
    private final Counter correlationIdInvalidReplaced;
    private final MeterRegistry registry;

    public GatewayMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.correlationIdMissing = Counter.builder("ecofy.gateway.correlation.missing")
                .description("Requisições recebidas sem header X-Correlation-Id")
                .register(registry);
        this.correlationIdInvalidReplaced = Counter.builder("ecofy.gateway.correlation.invalid")
                .description("Correlation IDs inválidos substituídos por um novo valor gerado")
                .register(registry);
    }

    public void correlationIdMissing() {
        correlationIdMissing.increment();
    }

    public void correlationIdInvalidReplaced() {
        correlationIdInvalidReplaced.increment();
    }

    /**
     * Conta um fallback técnico do gateway. {@code type} é um valor controlado
     * (nome do {@code GatewayErrorCode}), garantindo baixa cardinalidade.
     */
    public void fallback(String type) {
        Counter.builder("ecofy.gateway.fallback")
                .description("Fallbacks técnicos acionados pelo circuit breaker do gateway")
                .tag("type", type)
                .register(registry)
                .increment();
    }
}
