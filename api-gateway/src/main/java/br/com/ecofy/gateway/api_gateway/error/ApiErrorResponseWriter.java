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

// Centraliza a serialização e a escrita das respostas de erro do Gateway.
@Component
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Converte o erro em JSON e o escreve na resposta reativa.
    public Mono<Void> write(ServerHttpResponse response, ApiErrorResponse body) {
        response.setRawStatusCode(body.status());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        if (body.traceId() != null) {
            response.getHeaders().set(
                    GatewayHeaders.CORRELATION_ID,
                    body.traceId()
            );
        }

        byte[] bytes;

        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JacksonException exception) {
            bytes = ("{\"status\":" + body.status() + ",\"errorCode\":\""
                    + GatewayErrorCode.INTERNAL_GATEWAY_ERROR.name()
                    + "\",\"details\":[]}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}
