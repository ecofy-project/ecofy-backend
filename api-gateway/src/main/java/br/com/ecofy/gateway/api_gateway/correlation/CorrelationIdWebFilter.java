package br.com.ecofy.gateway.api_gateway.correlation;

import br.com.ecofy.gateway.api_gateway.metrics.GatewayMetrics;
import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

// Propaga o correlation ID pela requisição, resposta e contexto reativo.
@Component
public class CorrelationIdWebFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdWebFilter.class);

    private final CorrelationIdValidator validator;
    private final GatewayMetrics metrics;

    public CorrelationIdWebFilter(CorrelationIdValidator validator, GatewayMetrics metrics) {
        this.validator = validator;
        this.metrics = metrics;
    }

    // Resolve e propaga um correlation ID seguro durante o processamento da requisição.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String received = request.getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);

        String correlationId;

        if (received == null) {
            metrics.correlationIdMissing();
            correlationId = validator.generate();
        } else if (validator.isValid(received)) {
            correlationId = received.trim();
        } else {
            metrics.correlationIdInvalidReplaced();
            correlationId = validator.generate();
        }

        ServerHttpRequest mutated = request.mutate()
                .headers(headers -> headers.set(
                        GatewayHeaders.CORRELATION_ID,
                        correlationId
                ))
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutated)
                .build();

        mutatedExchange.getAttributes().put(
                GatewayHeaders.CORRELATION_ID_ATTR,
                correlationId
        );

        final String cid = correlationId;

        mutatedExchange.getResponse().beforeCommit(() -> {
            mutatedExchange.getResponse()
                    .getHeaders()
                    .set(GatewayHeaders.CORRELATION_ID, cid);

            return Mono.empty();
        });

        log.info(
                "[gateway] request method={} path={} correlationId={}",
                request.getMethod(),
                request.getPath().value(),
                cid
        );

        return chain.filter(mutatedExchange)
                .contextWrite(Context.of(
                        GatewayHeaders.CORRELATION_ID_CONTEXT_KEY,
                        cid
                ));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
