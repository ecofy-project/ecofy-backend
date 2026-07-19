package br.com.ecofy.gateway.api_gateway.correlation;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

// Configura a propagação do correlation ID entre o Reactor Context e o MDC.
@Configuration
public class ReactorContextConfig {

    // Registra o accessor e habilita a propagação automática do contexto reativo.
    @PostConstruct
    void enableContextPropagation() {
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor(new CorrelationIdThreadLocalAccessor());

        Hooks.enableAutomaticContextPropagation();
    }
}
