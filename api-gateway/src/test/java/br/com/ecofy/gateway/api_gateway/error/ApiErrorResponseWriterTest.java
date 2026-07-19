package br.com.ecofy.gateway.api_gateway.error;

import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários da escrita das respostas de erro")
class ApiErrorResponseWriterTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private DataBufferFactory bufferFactory;

    @Mock
    private DataBuffer dataBuffer;

    private ApiErrorResponseWriter writer;
    private HttpHeaders responseHeaders;

    @BeforeEach
    void setUp() {
        writer = new ApiErrorResponseWriter(objectMapper);
        responseHeaders = new HttpHeaders();

        when(response.getHeaders()).thenReturn(responseHeaders);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(bufferFactory.wrap(any(byte[].class))).thenReturn(dataBuffer);
        when(response.writeWith(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Deve serializar e escrever a resposta com o correlation ID")
    void write_respostaComTraceId_deveSerializarEEscreverCorpo() {
        // Arrange
        ApiErrorResponse body = createBody("trace-id-123");

        byte[] serializedBody = """
                {
                  "status": 503,
                  "errorCode": "DOWNSTREAM_UNAVAILABLE"
                }
                """.getBytes(StandardCharsets.UTF_8);

        when(objectMapper.writeValueAsBytes(body))
                .thenReturn(serializedBody);

        // Act
        Mono<Void> result = writer.write(response, body);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(responseHeaders.getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(responseHeaders.getFirst(GatewayHeaders.CORRELATION_ID))
                .isEqualTo("trace-id-123");

        verify(response).setRawStatusCode(503);
        verify(objectMapper).writeValueAsBytes(body);
        verify(bufferFactory).wrap(serializedBody);
        verify(response).writeWith(any());
    }

    @Test
    @DisplayName("Deve escrever a resposta sem adicionar o correlation ID quando o trace ID for nulo")
    void write_respostaSemTraceId_deveEscreverSemCorrelationId() {
        // Arrange
        ApiErrorResponse body = createBody(null);

        byte[] serializedBody = """
                {
                  "status": 503,
                  "errorCode": "DOWNSTREAM_UNAVAILABLE"
                }
                """.getBytes(StandardCharsets.UTF_8);

        when(objectMapper.writeValueAsBytes(body))
                .thenReturn(serializedBody);

        // Act
        Mono<Void> result = writer.write(response, body);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(responseHeaders.getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(responseHeaders.getFirst(GatewayHeaders.CORRELATION_ID))
                .isNull();

        verify(response).setRawStatusCode(503);
        verify(objectMapper).writeValueAsBytes(body);
        verify(bufferFactory).wrap(serializedBody);
        verify(response).writeWith(any());
    }

    @Test
    @DisplayName("Deve escrever o JSON de contingência quando a serialização falhar")
    void write_falhaNaSerializacao_deveEscreverJsonDeContingencia() {
        // Arrange
        ApiErrorResponse body = createBody("trace-id-error");
        JacksonException exception = mock(JacksonException.class);

        when(objectMapper.writeValueAsBytes(body))
                .thenThrow(exception);

        ArgumentCaptor<byte[]> bytesCaptor =
                ArgumentCaptor.forClass(byte[].class);

        // Act
        Mono<Void> result = writer.write(response, body);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setRawStatusCode(503);
        verify(objectMapper).writeValueAsBytes(body);
        verify(bufferFactory).wrap(bytesCaptor.capture());
        verify(response).writeWith(any());

        String fallbackJson = new String(
                bytesCaptor.getValue(),
                StandardCharsets.UTF_8
        );

        assertThat(fallbackJson).isEqualTo(
                "{\"status\":503,\"errorCode\":\"INTERNAL_GATEWAY_ERROR\",\"details\":[]}"
        );

        assertThat(responseHeaders.getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(responseHeaders.getFirst(GatewayHeaders.CORRELATION_ID))
                .isEqualTo("trace-id-error");
    }

    private ApiErrorResponse createBody(String traceId) {
        return new ApiErrorResponse(
                Instant.parse("2026-07-19T12:00:00Z"),
                503,
                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE.name(),
                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE.defaultMessage(),
                "/api/v1/users",
                traceId,
                List.of()
        );
    }
}
