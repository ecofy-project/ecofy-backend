package br.com.ecofy.gateway.api_gateway.correlation;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Habilita a propagação automática de contexto do Reactor e registra o accessor
 * que espelha o correlation ID no MDC (ECO-05, §6.5).
 *
 * Com isso, o valor gravado no Reactor Context pelo {@link CorrelationIdGlobalFilter}
 * fica disponível para o layout de log via {@code %X{correlationId}} nas threads
 * reativas, de forma compatível com WebFlux.
 */
@Configuration
public class ReactorContextConfig {

    @PostConstruct
    void enableContextPropagation() {
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor(new CorrelationIdThreadLocalAccessor());
        Hooks.enableAutomaticContextPropagation();
    }
}
