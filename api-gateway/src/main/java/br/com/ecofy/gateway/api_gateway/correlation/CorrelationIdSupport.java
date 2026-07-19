package br.com.ecofy.gateway.api_gateway.correlation;

import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import org.springframework.web.server.ServerWebExchange;

// Centraliza a recuperação do correlation ID associado à requisição.
public final class CorrelationIdSupport {

    private static final String UNKNOWN = "unknown";

    private CorrelationIdSupport() {
    }

    // Resolve o correlation ID pelo atributo do exchange ou pelo header da requisição.
    public static String resolve(ServerWebExchange exchange) {
        Object attr = exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR);

        if (attr instanceof String correlationId && !correlationId.isBlank()) {
            return correlationId;
        }

        String header = exchange.getRequest()
                .getHeaders()
                .getFirst(GatewayHeaders.CORRELATION_ID);

        if (header != null && !header.isBlank()) {
            return header;
        }

        return UNKNOWN;
    }
}
