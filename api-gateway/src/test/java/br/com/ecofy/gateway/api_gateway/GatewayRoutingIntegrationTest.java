package br.com.ecofy.gateway.api_gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração de roteamento estático.
 *
 * Sobe um backend "stub" (JDK HttpServer, sem dependências extras) que captura
 * a última requisição recebida. Todas as variáveis MS_*_URI apontam para esse
 * stub, de modo que qualquer prefixo público (/auth, /users, ...) é roteado
 * até ele. Assim validamos, através de HTTP real, o comportamento do gateway:
 *
 * - todas as rotas estáticas encaminham para o backend (200);
 * - o path é preservado (sem StripPrefix);
 * - o header Authorization é preservado e repassado downstream;
 * - a query string é preservada;
 * - o header Cookie é removido (default-filter RemoveRequestHeader=Cookie);
 * - o header X-Gateway é adicionado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIntegrationTest {

    private static HttpServer stub;
    private static volatile CapturedRequest last;

    private record CapturedRequest(String path, String rawQuery, HttpHeaders headers) {}

    @DynamicPropertySource
    static void backendProperties(DynamicPropertyRegistry registry) throws IOException {
        if (stub == null) {
            stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            stub.createContext("/", exchange -> {
                URI uri = exchange.getRequestURI();
                HttpHeaders headers = new HttpHeaders();
                exchange.getRequestHeaders().forEach((k, v) -> {
                    if (k != null) {
                        headers.addAll(k, v);
                    }
                });
                last = new CapturedRequest(uri.getPath(), uri.getRawQuery(), headers);

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
        registry.add("MS_AUTH_URI", () -> base);
        registry.add("MS_INGESTION_URI", () -> base);
        registry.add("MS_CATEGORIZATION_URI", () -> base);
        registry.add("MS_BUDGETING_URI", () -> base);
        registry.add("MS_INSIGHTS_URI", () -> base);
        registry.add("MS_NOTIFICATION_URI", () -> base);
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

    private WebTestClient webTestClient;

    @BeforeEach
    void reset() {
        last = null;
        // client apontando para o servidor vivo do gateway; timeout folgado para CI
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(15))
                .build();
    }

    @ParameterizedTest(name = "rota {0} encaminha para o backend")
    @CsvSource({
            "/auth/login",
            "/ingestion/upload",
            "/categorization/rules",
            "/budgeting/limits",
            "/insights/summary",
            "/notification/send",
            "/users/me"
    })
    void allStaticRoutesForwardToBackend(String path) {
        webTestClient.get().uri(path)
                .exchange()
                .expectStatus().isOk();

        assertThat(last).as("backend deve ter recebido a requisição para " + path).isNotNull();
        assertThat(last.path()).isEqualTo(path);
    }

    @Test
    void msAuthLoginRouteIsReachable() {
        webTestClient.post().uri("/auth/login")
                .exchange()
                .expectStatus().isOk();

        assertThat(last).isNotNull();
        assertThat(last.path()).isEqualTo("/auth/login");
    }

    @Test
    void authorizationHeaderIsPreservedDownstream() {
        webTestClient.get().uri("/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token-123")
                .exchange()
                .expectStatus().isOk();

        assertThat(last).isNotNull();
        assertThat(last.headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer test-token-123");
    }

    @Test
    void queryStringIsPreservedDownstream() {
        webTestClient.get().uri("/insights/summary?from=2026-01-01&to=2026-12-31&page=2")
                .exchange()
                .expectStatus().isOk();

        assertThat(last).isNotNull();
        assertThat(last.rawQuery()).isEqualTo("from=2026-01-01&to=2026-12-31&page=2");
    }

    @Test
    void cookieHeaderIsRemovedButAuthorizationIsKept() {
        webTestClient.get().uri("/users/me")
                .header(HttpHeaders.COOKIE, "session=abc; theme=dark")
                .header(HttpHeaders.AUTHORIZATION, "Bearer keep-me")
                .exchange()
                .expectStatus().isOk();

        assertThat(last).isNotNull();
        assertThat(last.headers().getFirst(HttpHeaders.COOKIE))
                .as("Cookie deve ser removido pelo default-filter").isNull();
        assertThat(last.headers().getFirst(HttpHeaders.AUTHORIZATION))
                .as("Authorization deve ser preservado").isEqualTo("Bearer keep-me");
    }

    @Test
    void gatewayMarkerHeaderIsAdded() {
        webTestClient.get().uri("/auth/jwks")
                .exchange()
                .expectStatus().isOk();

        assertThat(last).isNotNull();
        assertThat(last.headers().getFirst("X-Gateway")).isEqualTo("api-gateway");
    }
}
