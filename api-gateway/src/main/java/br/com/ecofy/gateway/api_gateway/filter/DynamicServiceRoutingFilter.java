package br.com.ecofy.gateway.api_gateway.filter;

import br.com.ecofy.gateway.api_gateway.config.DynamicGatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * Replica a ideia do artigo: /services/{service}/{path...}
 * 1) valida service
 * 2) monta upstream URI
 * 3) reescreve a request URL para o backend correto
 */
@Component
public class DynamicServiceRoutingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DynamicServiceRoutingFilter.class);

    private final DynamicGatewayProperties props;

    public DynamicServiceRoutingFilter(DynamicGatewayProperties props) {
        this.props = props;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String rawPath = exchange.getRequest().getURI().getPath();
        String prefix = props.getPrefix(); // "/services"

        // Só processa /services/**
        if (!rawPath.startsWith(prefix + "/")) {
            return chain.filter(exchange);
        }

        // Quebra path: /services/{service}/resto...
        String remainder = rawPath.substring((prefix + "/").length()); // "{service}/resto..."
        List<String> parts = List.of(remainder.split("/", -1));
        String service = parts.isEmpty() ? "" : parts.get(0);

        if (service.isBlank()) {
            return notFound(exchange, "Missing service segment. Expected /services/{service}/...");
        }

        URI baseUri = props.getServices().get(service);
        if (baseUri == null) {
            // Validator (igual ao artigo)
            return notFound(exchange, "Service '" + service + "' not found in gateway registry.");
        }

        // Monta o path interno do MS (resto após /services/{service})
        String innerPath = remainder.substring(service.length()); // começa com "" ou "/..."
        if (innerPath.isBlank()) innerPath = "/";

        // Preserva query string
        String query = exchange.getRequest().getURI().getRawQuery();
        String target = baseUri.toString() + innerPath + (query != null && !query.isBlank() ? "?" + query : "");

        URI targetUri = URI.create(target);

        // Dispatcher: sobrescreve a URL do request para o gateway encaminhar
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, targetUri);
        exchange.getAttributes().put("dynamic.service", service);
        exchange.getAttributes().put("dynamic.target", targetUri.toString());

        return chain.filter(exchange);
    }

    private Mono<Void> notFound(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String cid = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        String path = exchange.getRequest().getURI().getPath();

        String body = """
                {
                  "status": 404,
                  "error": "NOT_FOUND",
                  "message": "%s",
                  "path": "%s",
                  "correlationId": "%s"
                }
                """.formatted(escapeJson(message), escapeJson(path), escapeJson(cid == null ? "" : cid));

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public int getOrder() {
        // precisa rodar ANTES do routing final do gateway
        return -200;
    }
}