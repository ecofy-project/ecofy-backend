package br.com.ecofy.gateway.api_gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração do correlation ID (ECO-05 §12.1) através de HTTP real.
 *
 * Valida: geração quando ausente, preservação de valor válido, substituição de
 * valores inseguros (vazio, longo demais, caracteres inválidos), propagação ao
 * downstream, retorno no header de resposta e o incremento da métrica de
 * requisições sem correlation ID (ECO-16 §12.8).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorrelationIdIntegrationTest {

    private static final String HEADER = "X-Correlation-Id";

    private static HttpServer stub;
    private static volatile String lastDownstreamCorrelationId;

    @DynamicPropertySource
    static void backendProperties(DynamicPropertyRegistry registry) throws IOException {
        if (stub == null) {
            stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            stub.createContext("/", exchange -> {
                lastDownstreamCorrelationId = exchange.getRequestHeaders().getFirst(HEADER);
                byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            stub.start();
        }
        String base = "http://127.0.0.1:" + stub.getAddress().getPort();
        registry.add("MS_USERS_URI", () -> base);
    }

    @AfterAll
    static void tearDown() {
        if (stub != null) {
            stub.stop(0);
            stub = null;
        }
    }

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        lastDownstreamCorrelationId = null;
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Test
    void generatesCorrelationIdWhenAbsent() {
        String responseId = client.get().uri("/users/me")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(HEADER)
                .returnResult(Void.class)
                .getResponseHeaders().getFirst(HEADER);

        assertThat(responseId).isNotBlank();
        assertThat(UUID.fromString(responseId)).isNotNull(); // gerado -> UUID válido
        assertThat(lastDownstreamCorrelationId)
                .as("mesmo ID propagado ao downstream").isEqualTo(responseId);
    }

    @Test
    void preservesValidClientCorrelationId() {
        String clientId = "req-abc_123.9";
        String responseId = client.get().uri("/users/me")
                .header(HEADER, clientId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HEADER, clientId)
                .returnResult(Void.class)
                .getResponseHeaders().getFirst(HEADER);

        assertThat(responseId).isEqualTo(clientId);
        assertThat(lastDownstreamCorrelationId).isEqualTo(clientId);
    }

    @Test
    void replacesEmptyCorrelationId() {
        String responseId = client.get().uri("/users/me")
                .header(HEADER, "")
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class)
                .getResponseHeaders().getFirst(HEADER);

        assertThat(responseId).isNotBlank();
        assertThat(UUID.fromString(responseId)).isNotNull();
        assertThat(lastDownstreamCorrelationId).isEqualTo(responseId);
    }

    @Test
    void replacesTooLongCorrelationId() {
        String tooLong = "a".repeat(200);
        String responseId = client.get().uri("/users/me")
                .header(HEADER, tooLong)
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class)
                .getResponseHeaders().getFirst(HEADER);

        assertThat(responseId).isNotEqualTo(tooLong);
        assertThat(UUID.fromString(responseId)).isNotNull();
        assertThat(lastDownstreamCorrelationId).isEqualTo(responseId);
    }

    @Test
    void replacesUnsafeCorrelationId() {
        String unsafe = "has spaces and ; chars";
        String responseId = client.get().uri("/users/me")
                .header(HEADER, unsafe)
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class)
                .getResponseHeaders().getFirst(HEADER);

        assertThat(responseId).isNotEqualTo(unsafe);
        assertThat(UUID.fromString(responseId)).isNotNull();
        assertThat(lastDownstreamCorrelationId)
                .as("nunca propaga conteúdo inseguro").isEqualTo(responseId);
    }

    @Test
    void missingCorrelationIdIncrementsMetric() {
        client.get().uri("/users/me").exchange().expectStatus().isOk();

        String prometheus = client.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(prometheus).contains("ecofy_gateway_correlation_missing");
    }
}
