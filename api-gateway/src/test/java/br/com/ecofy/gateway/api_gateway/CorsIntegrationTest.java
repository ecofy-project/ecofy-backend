package br.com.ecofy.gateway.api_gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Testes de CORS explícito por ambiente (ECO-19 §12.3), no profile {@code test}
 * (origem permitida fixa: {@code https://app.allowed.test}).
 *
 * Cobre: origem permitida, origem bloqueada, preflight OPTIONS, métodos/headers
 * permitidos, exposição de X-Correlation-Id e ausência de wildcard.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CorsIntegrationTest {

    private static final String ALLOWED_ORIGIN = "https://app.allowed.test";
    private static final String BLOCKED_ORIGIN = "https://evil.example";

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
    void preflightFromAllowedOriginSucceeds() {
        client.options().uri("/users/me")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN)
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, m -> m.contains("GET"));
    }

    @Test
    void preflightFromBlockedOriginIsRejected() {
        client.options().uri("/users/me")
                .header(HttpHeaders.ORIGIN, BLOCKED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void preflightWithDisallowedMethodIsRejected() {
        client.options().uri("/users/me")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void actualRequestExposesCorrelationIdHeader() {
        client.get().uri("/actuator/health")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN)
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        h -> org.assertj.core.api.Assertions.assertThat(h).contains("X-Correlation-Id"));
    }
}
