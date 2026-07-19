package br.com.ecofy.gateway.api_gateway.correlation;

import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;

// Propaga o correlation ID entre o Reactor Context e o MDC.
public class CorrelationIdThreadLocalAccessor implements ThreadLocalAccessor<String> {

    @Override
    public Object key() {
        return GatewayHeaders.CORRELATION_ID_CONTEXT_KEY;
    }

    @Override
    public String getValue() {
        return MDC.get(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY);
    }

    @Override
    public void setValue(String value) {
        MDC.put(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY, value);
    }

    @Override
    public void setValue() {
        MDC.remove(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY);
    }

    @Override
    public void restore() {
        MDC.remove(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY);
    }
}
