package br.com.ecofy.gateway.api_gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * Testes do padrão de versionamento externo /api/v1 (ECO-23 §12.7).
 *
 * Um backend stub captura a última requisição recebida. Confirma que o path
 * externo versionado é reescrito para o contrato interno REAL de cada serviço
 * (RewritePath), preservando query string, header Authorization e correlation ID.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VersioningIntegrationTest {

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
        registry.add("MS_USERS_URI", () -> base);
        registry.add("MS_INSIGHTS_URI", () -> base);
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
        last = null;
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Test
    void authExternalPathIsRewrittenToInternalContractIncludingContextPath() {
        client.get().uri("/api/v1/auth/token")
                .exchange()
                .expectStatus().isOk();

        assertThat(last).isNotNull();
        // ECO-23: o ms-auth roda sob context-path /auth e expõe /api/v1/auth/**,
        // então o path interno real inclui o context-path.
        assertThat(last.path()).isEqualTo("/auth/api/v1/auth/token");
    }

    @Test
    void usersExternalPathIsRewrittenToInternalContractIncludingContextPath() {
        client.get().uri("/api/v1/users/profile/me")
                .exchange()
                .expectStatus().isOk();

        assertThat(last).isNotNull();
        // ECO-23: o ms-users roda sob context-path /users e mapeia /api/users/v1/**,
        // então o path interno real inclui o context-path. Sem isso o gateway dava 404.
        assertThat(last.path()).isEqualTo("/users/api/users/v1/profile/me");
    }

    @Test
    void queryStringAndAuthorizationArePreservedThroughVersionedRoute() {
        client.get().uri("/api/v1/insights/summary?from=2026-01-01&page=2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer keep-me")
                .exchange()
                .expectStatus().isOk();

        assertThat(last).isNotNull();
        // Todos os MS rodam sob context-path, então o rewrite o inclui (ver ECO-23).
        assertThat(last.path()).isEqualTo("/insights/api/insights/v1/summary");
        assertThat(last.rawQuery()).isEqualTo("from=2026-01-01&page=2");
        assertThat(last.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer keep-me");
        assertThat(last.headers().getFirst("X-Correlation-Id")).isNotBlank();
    }
}
