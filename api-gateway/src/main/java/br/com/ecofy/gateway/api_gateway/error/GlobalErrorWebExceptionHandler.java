package br.com.ecofy.gateway.api_gateway.error;

import br.com.ecofy.gateway.api_gateway.correlation.CorrelationIdSupport;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

// Converte exceções não tratadas em respostas padronizadas do Gateway.
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler, Ordered {

    private static final Logger log = LoggerFactory.getLogger(
            GlobalErrorWebExceptionHandler.class
    );

    private final ApiErrorResponseWriter writer;

    public GlobalErrorWebExceptionHandler(ApiErrorResponseWriter writer) {
        this.writer = writer;
    }

    // Trata a exceção e escreve uma resposta segura com o status correspondente.
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        String correlationId = CorrelationIdSupport.resolve(exchange);
        String path = exchange.getRequest().getPath().value();
        GatewayErrorCode code = classify(ex);

        log.warn(
                "[gateway] handled error correlationId={} method={} path={} errorCode={} exception={} detail={}",
                correlationId,
                exchange.getRequest().getMethod(),
                path,
                code.name(),
                ex.getClass().getName(),
                safeDetail(ex)
        );

        ApiErrorResponse body = ApiErrorResponse.of(
                code,
                path,
                correlationId
        );

        return writer.write(exchange.getResponse(), body);
    }

    // Classifica a exceção conforme o erro técnico identificado na cadeia de causas.
    private GatewayErrorCode classify(Throwable ex) {
        Throwable current = ex;

        while (current != null) {
            if (current instanceof CallNotPermittedException) {
                return GatewayErrorCode.CIRCUIT_BREAKER_OPEN;
            }

            if (current instanceof TimeoutException) {
                return GatewayErrorCode.GATEWAY_TIMEOUT;
            }

            if (current instanceof ConnectException) {
                return GatewayErrorCode.DOWNSTREAM_UNAVAILABLE;
            }

            if (current instanceof ResponseStatusException rse) {
                return fromStatus(rse.getStatusCode().value());
            }

            current = current.getCause();
        }

        return GatewayErrorCode.INTERNAL_GATEWAY_ERROR;
    }

    // Converte o status HTTP no código de erro correspondente.
    private GatewayErrorCode fromStatus(int status) {
        if (status == HttpStatus.NOT_FOUND.value()) {
            return GatewayErrorCode.ROUTE_NOT_FOUND;
        }

        if (status == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            return GatewayErrorCode.METHOD_NOT_ALLOWED;
        }

        if (status == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            return GatewayErrorCode.DOWNSTREAM_UNAVAILABLE;
        }

        if (status == HttpStatus.GATEWAY_TIMEOUT.value()) {
            return GatewayErrorCode.GATEWAY_TIMEOUT;
        }

        if (status >= 400 && status < 500) {
            return GatewayErrorCode.INVALID_REQUEST;
        }

        return GatewayErrorCode.INTERNAL_GATEWAY_ERROR;
    }

    // Sanitiza a mensagem técnica antes de registrá-la no log.
    private String safeDetail(Throwable ex) {
        String message = ex.getMessage();

        if (message == null) {
            return "";
        }

        return message.replaceAll("[\\r\\n]+", " ");
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
