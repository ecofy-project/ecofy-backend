package br.com.ecofy.gateway.api_gateway.correlation;

import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import reactor.core.publisher.Hooks;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes unitários da configuração do contexto reativo")
class ReactorContextConfigTest {

    private final ContextRegistry contextRegistry = ContextRegistry.getInstance();

    private ReactorContextConfig reactorContextConfig;

    @BeforeEach
    void setUp() {
        contextRegistry.removeThreadLocalAccessor(
                GatewayHeaders.CORRELATION_ID_CONTEXT_KEY
        );
        Hooks.disableAutomaticContextPropagation();
        MDC.clear();

        reactorContextConfig = new ReactorContextConfig();
    }

    @AfterEach
    void tearDown() {
        contextRegistry.removeThreadLocalAccessor(
                GatewayHeaders.CORRELATION_ID_CONTEXT_KEY
        );
        Hooks.disableAutomaticContextPropagation();
        MDC.clear();
    }

    @Test
    @DisplayName("Deve registrar o accessor responsável pela propagação do correlation ID")
    void enableContextPropagation_chamadaRealizada_deveRegistrarThreadLocalAccessor() {
        // Act
        reactorContextConfig.enableContextPropagation();

        // Assert
        ThreadLocalAccessor<?> accessor = contextRegistry
                .getThreadLocalAccessors()
                .stream()
                .filter(candidate -> GatewayHeaders.CORRELATION_ID_CONTEXT_KEY.equals(
                        candidate.key()
                ))
                .findFirst()
                .orElse(null);

        assertThat(accessor)
                .isNotNull()
                .isInstanceOf(CorrelationIdThreadLocalAccessor.class);

        assertThat(accessor.key())
                .isEqualTo(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY);
    }
}
