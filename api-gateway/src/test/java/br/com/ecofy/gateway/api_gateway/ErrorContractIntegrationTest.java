package br.com.ecofy.gateway.api_gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do contrato padrão de erro do gateway (ECO-04 §12.2).
 *
 * Usa uma rota inexistente (404) para exercitar o {@code GlobalErrorWebExceptionHandler}
 * e verifica: corpo {@code ApiErrorResponse}, {@code details} como {@code []},
 * {@code timestamp} em UTC (sufixo "Z"), status HTTP coerente com o body,
 * {@code errorCode} estável e preservação do correlation ID no campo {@code traceId}
 * e no header de resposta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorContractIntegrationTest {

    private static final String HEADER = "X-Correlation-Id";

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void unknownRouteReturnsApiErrorResponse() {
        client.get().uri("/rota-inexistente/xyz")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("ROUTE_NOT_FOUND")
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.path").isEqualTo("/rota-inexistente/xyz")
                .jsonPath("$.traceId").isNotEmpty()
                .jsonPath("$.details").isArray()
                .jsonPath("$.details").isEmpty()
                .jsonPath("$.timestamp").value(ts -> assertThat((String) ts).endsWith("Z"));
    }

    @Test
    void errorPreservesClientCorrelationId() {
        String clientId = "err-trace-42";
        client.get().uri("/rota-inexistente/xyz")
                .header(HEADER, clientId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectHeader().valueEquals(HEADER, clientId)
                .expectBody()
                .jsonPath("$.traceId").isEqualTo(clientId);
    }
}
