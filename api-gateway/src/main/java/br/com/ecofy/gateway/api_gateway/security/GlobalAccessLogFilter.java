package br.com.ecofy.gateway.api_gateway.security;

import br.com.ecofy.gateway.api_gateway.core.application.service.GatewayLoggingService;
import br.com.ecofy.gateway.api_gateway.core.domain.TenantContext;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

@Component
public class GlobalAccessLogFilter implements GlobalFilter, Ordered {

    private final GatewayLoggingService loggingService;

    public GlobalAccessLogFilter(GatewayLoggingService loggingService) {
        this.loggingService = loggingService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        long start = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long latency = System.currentTimeMillis() - start;

                    ServerHttpRequest req = exchange.getRequest();
                    String method = req.getMethod() != null ? req.getMethod().name() : "UNKNOWN";
                    String path = req.getURI().getPath();
                    String query = req.getURI().getQuery();
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    String upstreamServiceId = resolveRouteId(exchange);
                    InetSocketAddress remoteAddress = resolveRemoteAddress(req);

                    Map<String, String> tags = new HashMap<>();
                    String traceId = firstHeader(req.getHeaders(), "x-trace-id");
                    if (traceId != null) tags.put("traceId", traceId);

                    // Neste cenário "roteador puro", TenantContext pode ser null.
                    // Se futuramente você resolver token, preencha aqui.
                    TenantContext tenant = null;

                    loggingService.logAccess(
                            tenant,
                            method,
                            path,
                            query,
                            status,
                            latency,
                            upstreamServiceId,
                            remoteAddress,
                            tags
                    );
                });
    }

    private static String resolveRouteId(ServerWebExchange exchange) {
        Object routeAttr = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (routeAttr instanceof Route route) {
            return route.getId();
        }
        return "unknown-route";
    }

    private static InetSocketAddress resolveRemoteAddress(ServerHttpRequest req) {
        // Se estiver atrás de proxy/reverse proxy, você pode preferir X-Forwarded-For
        return req.getRemoteAddress();
    }

    private static String firstHeader(HttpHeaders headers, String name) {
        String v = headers.getFirst(name);
        return (v == null || v.isBlank()) ? null : v;
    }

    @Override
    public int getOrder() {
        // roda após roteamento já estar decidido, mas ainda no fluxo global
        return Ordered.LOWEST_PRECEDENCE;
    }
}
