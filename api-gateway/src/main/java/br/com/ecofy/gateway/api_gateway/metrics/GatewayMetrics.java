package br.com.ecofy.gateway.api_gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

// Centraliza as métricas técnicas customizadas do Gateway.
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

    // Registra uma requisição recebida sem correlation ID.
    public void correlationIdMissing() {
        correlationIdMissing.increment();
    }

    // Registra a substituição de um correlation ID inválido.
    public void correlationIdInvalidReplaced() {
        correlationIdInvalidReplaced.increment();
    }

    // Registra o acionamento de um fallback técnico por tipo de erro.
    public void fallback(String type) {
        Counter.builder("ecofy.gateway.fallback")
                .description("Fallbacks técnicos acionados pelo circuit breaker do gateway")
                .tag("type", type)
                .register(registry)
                .increment();
    }
}
