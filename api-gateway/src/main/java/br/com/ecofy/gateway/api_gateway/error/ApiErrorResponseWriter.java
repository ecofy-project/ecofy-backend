package br.com.ecofy.gateway.api_gateway.error;

import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * Serializa um {@link ApiErrorResponse} para a resposta reativa, de forma
 * não-bloqueante (ECO-04). Centraliza a escrita usada pelo handler global de
 * erro, garantindo Content-Type, status e header de correlação consistentes.
 *
 * Usa o {@code ObjectMapper} do Jackson 3 (tools.jackson), padrão do Spring Boot 4,
 * que serializa {@link java.time.Instant} em ISO-8601 UTC ("...Z").
 */
@Component
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerHttpResponse response, ApiErrorResponse body) {
        response.setRawStatusCode(body.status());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (body.traceId() != null) {
            response.getHeaders().set(GatewayHeaders.CORRELATION_ID, body.traceId());
        }
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JacksonException e) {
            bytes = ("{\"status\":" + body.status() + ",\"errorCode\":\""
                    + GatewayErrorCode.INTERNAL_GATEWAY_ERROR.name() + "\",\"details\":[]}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
