package br.com.ecofy.gateway.api_gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes unitários das métricas técnicas do Gateway")
class GatewayMetricsTest {

    private SimpleMeterRegistry registry;
    private GatewayMetrics gatewayMetrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        gatewayMetrics = new GatewayMetrics(registry);
    }

    @AfterEach
    void tearDown() {
        registry.close();
    }

    @Test
    @DisplayName("Deve registrar os contadores de correlation ID durante a inicialização")
    void constructor_instanciaCriada_deveRegistrarContadoresDeCorrelationId() {
        // Act
        Counter missingCounter = registry
                .find("ecofy.gateway.correlation.missing")
                .counter();

        Counter invalidCounter = registry
                .find("ecofy.gateway.correlation.invalid")
                .counter();

        // Assert
        assertThat(missingCounter).isNotNull();
        assertThat(missingCounter.count()).isZero();
        assertThat(missingCounter.getId().getDescription())
                .isEqualTo("Requisições recebidas sem header X-Correlation-Id");

        assertThat(invalidCounter).isNotNull();
        assertThat(invalidCounter.count()).isZero();
        assertThat(invalidCounter.getId().getDescription())
                .isEqualTo(
                        "Correlation IDs inválidos substituídos por um novo valor gerado"
                );
    }

    @Test
    @DisplayName("Deve incrementar o contador de requisições sem correlation ID")
    void correlationIdMissing_multiplasChamadas_deveIncrementarContador() {
        // Act
        gatewayMetrics.correlationIdMissing();
        gatewayMetrics.correlationIdMissing();

        // Assert
        Counter counter = registry
                .get("ecofy.gateway.correlation.missing")
                .counter();

        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Deve incrementar o contador de correlation IDs inválidos")
    void correlationIdInvalidReplaced_multiplasChamadas_deveIncrementarContador() {
        // Act
        gatewayMetrics.correlationIdInvalidReplaced();
        gatewayMetrics.correlationIdInvalidReplaced();
        gatewayMetrics.correlationIdInvalidReplaced();

        // Assert
        Counter counter = registry
                .get("ecofy.gateway.correlation.invalid")
                .counter();

        assertThat(counter.count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Deve registrar e incrementar o fallback conforme o tipo de erro")
    void fallback_tiposInformados_deveRegistrarContadoresSeparados() {
        // Act
        gatewayMetrics.fallback("DOWNSTREAM_UNAVAILABLE");
        gatewayMetrics.fallback("DOWNSTREAM_UNAVAILABLE");
        gatewayMetrics.fallback("GATEWAY_TIMEOUT");

        // Assert
        Counter unavailableCounter = registry
                .get("ecofy.gateway.fallback")
                .tag("type", "DOWNSTREAM_UNAVAILABLE")
                .counter();

        Counter timeoutCounter = registry
                .get("ecofy.gateway.fallback")
                .tag("type", "GATEWAY_TIMEOUT")
                .counter();

        assertThat(unavailableCounter.count()).isEqualTo(2.0);
        assertThat(timeoutCounter.count()).isEqualTo(1.0);

        assertThat(unavailableCounter.getId().getDescription())
                .isEqualTo(
                        "Fallbacks técnicos acionados pelo circuit breaker do gateway"
                );
    }

    @Test
    @DisplayName("Deve reutilizar o mesmo contador para chamadas com o mesmo tipo de fallback")
    void fallback_mesmoTipo_deveReutilizarContadorRegistrado() {
        // Arrange
        String type = "CIRCUIT_BREAKER_OPEN";

        // Act
        gatewayMetrics.fallback(type);
        Counter firstCounter = registry
                .get("ecofy.gateway.fallback")
                .tag("type", type)
                .counter();

        gatewayMetrics.fallback(type);
        Counter secondCounter = registry
                .get("ecofy.gateway.fallback")
                .tag("type", type)
                .counter();

        // Assert
        assertThat(secondCounter).isSameAs(firstCounter);
        assertThat(secondCounter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o tipo do fallback for nulo")
    void fallback_tipoNulo_deveLancarNullPointerException() {
        // Act e Assert
        assertThatThrownBy(() -> gatewayMetrics.fallback(null))
                .isInstanceOf(NullPointerException.class);
    }
}
