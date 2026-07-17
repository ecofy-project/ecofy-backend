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

/**
 * Resolve, propaga e registra o correlation ID de cada requisição (ECO-05).
 *
 * É um {@link WebFilter} (não um GlobalFilter do gateway) para rodar em TODAS as
 * requisições, inclusive rotas não casadas (404) e antes do roteamento. Garante
 * que o ID já esteja no request encaminhado ao downstream, no atributo do exchange
 * (usado por erro/fallback), no header de resposta e no Reactor Context (chave
 * usada para MDC nos logs reativos).
 *
 * Ordem: {@link Ordered#HIGHEST_PRECEDENCE}. Roda antes dos demais filtros; para
 * preflight OPTIONS o CorsWebFilter encerra a cadeia depois, o que é inofensivo.
 *
 * Estratégia de MDC no Reactor: o ID é gravado no Reactor Context via
 * {@code contextWrite}. Com a propagação automática de contexto habilitada em
 * {@link ReactorContextConfig} e o {@code CorrelationIdThreadLocalAccessor}, o
 * valor aparece em {@code %X{correlationId}} sem uso incorreto de ThreadLocal.
 * Um log estruturado explícito também é emitido aqui — a rastreabilidade mínima
 * não depende só do MDC.
 */
@Component
public class CorrelationIdWebFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdWebFilter.class);

    private final CorrelationIdValidator validator;
    private final GatewayMetrics metrics;

    public CorrelationIdWebFilter(CorrelationIdValidator validator, GatewayMetrics metrics) {
        this.validator = validator;
        this.metrics = metrics;
    }

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

        // Request encaminhado ao downstream carrega sempre um único valor seguro.
        ServerHttpRequest mutated = request.mutate()
                .headers(h -> h.set(GatewayHeaders.CORRELATION_ID, correlationId))
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutated).build();
        mutatedExchange.getAttributes().put(GatewayHeaders.CORRELATION_ID_ATTR, correlationId);

        final String cid = correlationId;
        mutatedExchange.getResponse().beforeCommit(() -> {
            mutatedExchange.getResponse().getHeaders().set(GatewayHeaders.CORRELATION_ID, cid);
            return Mono.empty();
        });

        // Nunca loga Authorization, cookies ou query string.
        log.info("[gateway] request method={} path={} correlationId={}",
                request.getMethod(), request.getPath().value(), cid);

        return chain.filter(mutatedExchange)
                .contextWrite(Context.of(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY, cid));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
