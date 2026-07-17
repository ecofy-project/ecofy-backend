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
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de resiliência do gateway (ECO-21 §12.4/12.5 + ECO-16 §12.8).
 *
 * Backend stub controlável: caminhos "/.../slow" dormem além do response-timeout
 * (força timeout); demais respostas contam as chamadas e devolvem o status pedido
 * (default 500) para exercitar a política de retry. Uma rota aponta para uma porta
 * morta para simular indisponibilidade (connection refused). Verifica timeout
 * controlado (504), fallback de indisponibilidade (503), retry apenas para GET
 * elegível (não para POST, não para 4xx) e exposição das métricas de fallback.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResilienceIntegrationTest {

    private static HttpServer stub;
    private static final AtomicInteger backendHits = new AtomicInteger();

    @DynamicPropertySource
    static void backendProperties(DynamicPropertyRegistry registry) throws IOException {
        if (stub == null) {
            stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            stub.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (path.contains("slow")) {
                    // Caminho de timeout: o gateway desiste em 4s, mas este handler só
                    // termina depois. NÃO conta a chamada para não poluir, com um
                    // incremento atrasado, a contagem dos testes de retry.
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    respond(exchange, 200, "{\"slow\":true}");
                    return;
                }
                backendHits.incrementAndGet();
                String forced = exchange.getRequestHeaders().getFirst("X-Test-Status");
                int status = forced != null ? Integer.parseInt(forced) : 500;
                respond(exchange, status, "{\"stub\":true}");
            });
            stub.start();
        }
        String base = "http://127.0.0.1:" + stub.getAddress().getPort();
        // Rotas vivas (timeout + retry) apontam para o stub.
        registry.add("MS_AUTH_URI", () -> base);
        // Rota de indisponibilidade aponta para uma porta reservada e liberada (recusa conexão).
        registry.add("MS_INGESTION_URI", () -> "http://127.0.0.1:" + deadPort());
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String json)
            throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        try {
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } catch (IOException ignored) {
            // Cliente (gateway) já pode ter desistido após timeout; nada a fazer.
        }
    }

    private static int deadPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
        backendHits.set(0);
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Test
    void slowDownstreamReturnsControlledTimeout() {
        client.get().uri("/api/v1/auth/slow")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("GATEWAY_TIMEOUT")
                .jsonPath("$.status").isEqualTo(504)
                .jsonPath("$.details").isEmpty()
                .jsonPath("$.traceId").isNotEmpty();
    }

    @Test
    void unavailableDownstreamReturnsControlledFallback() {
        client.get().uri("/api/v1/ingestion/anything")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("DOWNSTREAM_UNAVAILABLE")
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.details").isEmpty()
                .jsonPath("$.traceId").isNotEmpty();
    }

    @Test
    void getIsRetriedOnServerError() {
        client.get().uri("/api/v1/auth/retry")
                .exchange()
                .expectStatus().isEqualTo(500);

        // retries=2 -> 1 tentativa original + 2 retries = 3 chamadas ao backend.
        assertThat(backendHits.get()).isEqualTo(3);
    }

    @Test
    void postIsNotRetried() {
        client.post().uri("/api/v1/auth/retry")
                .exchange()
                .expectStatus().isEqualTo(500);

        assertThat(backendHits.get()).isEqualTo(1);
    }

    @Test
    void functional4xxIsNotRetried() {
        client.get().uri("/api/v1/auth/retry")
                .header("X-Test-Status", "400")
                .exchange()
                .expectStatus().isEqualTo(400);

        assertThat(backendHits.get()).isEqualTo(1);
    }

    @Test
    void fallbackMetricIsExposed() {
        client.get().uri("/api/v1/ingestion/anything").exchange();

        String prometheus = client.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(prometheus).contains("ecofy_gateway_fallback");
        assertThat(prometheus).contains("resilience4j_circuitbreaker");
    }
}
