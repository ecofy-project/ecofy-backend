package br.com.ecofy.gateway.api_gateway.correlation;

import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import org.springframework.web.server.ServerWebExchange;

/**
 * Acesso ao correlation ID resolvido para a requisição corrente, usado pelo
 * handler global de erro e pelo fallback do circuit breaker (ECO-05).
 *
 * O {@link CorrelationIdWebFilter} roda com a maior precedência e grava o ID
 * como atributo do exchange; por isso o atributo normalmente já existe. Como
 * salvaguarda, cai para o header da requisição e, por último, para um marcador
 * estável — o handler de erro nunca deve falhar por ausência de correlation ID.
 */
public final class CorrelationIdSupport {

    private static final String UNKNOWN = "unknown";

    private CorrelationIdSupport() {
    }

    public static String resolve(ServerWebExchange exchange) {
        Object attr = exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR);
        if (attr instanceof String s && !s.isBlank()) {
            return s;
        }
        String header = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
        if (header != null && !header.isBlank()) {
            return header;
        }
        return UNKNOWN;
    }
}
