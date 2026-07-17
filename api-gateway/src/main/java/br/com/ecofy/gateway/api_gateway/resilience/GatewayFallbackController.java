package br.com.ecofy.gateway.api_gateway.resilience;

import br.com.ecofy.gateway.api_gateway.correlation.CorrelationIdSupport;
import br.com.ecofy.gateway.api_gateway.error.ApiErrorResponse;
import br.com.ecofy.gateway.api_gateway.error.GatewayErrorCode;
import br.com.ecofy.gateway.api_gateway.metrics.GatewayMetrics;
import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.URI;
import java.util.Collection;

/**
 * Fallback técnico do circuit breaker do gateway (ECO-21 §8.5).
 *
 * As rotas versionadas configuram {@code CircuitBreaker} com
 * {@code fallbackUri: forward:/__gateway/fallback}. Quando a chamada falha (CB
 * aberto, timeout, indisponibilidade downstream), o gateway encaminha para cá.
 * Este fallback é técnico, não de negócio: nunca simula sucesso nem inventa dados
 * de domínio. Retorna o contrato {@link ApiErrorResponse}, preserva o correlation
 * ID, usa status HTTP coerente e registra a métrica de fallback.
 */
@RestController
public class GatewayFallbackController {

    private final GatewayMetrics metrics;

    public GatewayFallbackController(GatewayMetrics metrics) {
        this.metrics = metrics;
    }

    @RequestMapping("/__gateway/fallback")
    public Mono<ResponseEntity<ApiErrorResponse>> fallback(ServerWebExchange exchange) {
        Throwable cause = exchange.getAttribute(
                ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        GatewayErrorCode code = classify(cause);

        String correlationId = CorrelationIdSupport.resolve(exchange);
        String path = originalPath(exchange);

        metrics.fallback(code.name());

        ApiErrorResponse body = ApiErrorResponse.of(code, path, correlationId);
        return Mono.just(ResponseEntity
                .status(code.status())
                .header(GatewayHeaders.CORRELATION_ID, correlationId)
                .body(body));
    }

    private GatewayErrorCode classify(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof CallNotPermittedException) {
                return GatewayErrorCode.CIRCUIT_BREAKER_OPEN;
            }
            // O gateway mapeia timeout de rota para ResponseStatusException(504);
            // conexão recusada surge como ConnectException. Ver NettyRoutingFilter.
            if (current instanceof ResponseStatusException rse) {
                return fromStatus(rse.getStatusCode().value());
            }
            if (current instanceof org.springframework.cloud.gateway.support.TimeoutException
                    || current instanceof java.util.concurrent.TimeoutException) {
                return GatewayErrorCode.GATEWAY_TIMEOUT;
            }
            if (current instanceof ConnectException) {
                return GatewayErrorCode.DOWNSTREAM_UNAVAILABLE;
            }
            current = current.getCause();
        }
        // Sem causa específica (ex.: conexão encerrada/IO) → indisponibilidade downstream.
        return GatewayErrorCode.DOWNSTREAM_UNAVAILABLE;
    }

    private GatewayErrorCode fromStatus(int status) {
        if (status == HttpStatus.GATEWAY_TIMEOUT.value()) {
            return GatewayErrorCode.GATEWAY_TIMEOUT;
        }
        if (status == HttpStatus.NOT_FOUND.value()) {
            return GatewayErrorCode.ROUTE_NOT_FOUND;
        }
        if (status == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            return GatewayErrorCode.METHOD_NOT_ALLOWED;
        }
        // Falhas de transporte/5xx a partir do circuit breaker → indisponibilidade downstream.
        return GatewayErrorCode.DOWNSTREAM_UNAVAILABLE;
    }

    private String originalPath(ServerWebExchange exchange) {
        // GATEWAY_ORIGINAL_REQUEST_URL_ATTR guarda um Set<URI> (histórico de rewrites);
        // o primeiro elemento é a URL externa original.
        URI original = firstUri(exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR));
        if (original != null && original.getPath() != null) {
            return original.getPath();
        }
        return exchange.getRequest().getPath().value();
    }

    private URI firstUri(Object attr) {
        if (attr instanceof URI uri) {
            return uri;
        }
        if (attr instanceof Collection<?> collection && !collection.isEmpty()) {
            Object first = collection.iterator().next();
            if (first instanceof URI uri) {
                return uri;
            }
        }
        return null;
    }
}
