package br.com.ecofy.gateway.api_gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do circuit breaker do gateway (ECO-21 §12.6 + ECO-16 §12.8).
 *
 * Thresholds reduzidos por profile de teste (via propriedades dinâmicas) e uma
 * rota apontando para uma porta morta: as primeiras chamadas falham por
 * indisponibilidade (DOWNSTREAM_UNAVAILABLE) e, atingido o mínimo, o circuit
 * breaker abre — as chamadas seguintes retornam CIRCUIT_BREAKER_OPEN via fallback.
 * Também verifica a exposição das métricas do Resilience4j no Prometheus.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CircuitBreakerIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("MS_AUTH_URI", () -> "http://127.0.0.1:" + deadPort());
        // Abre o circuito rapidamente e de forma determinística.
        registry.add("ecofy.gateway.resilience.circuit-breaker.sliding-window-size", () -> 3);
        registry.add("ecofy.gateway.resilience.circuit-breaker.minimum-number-of-calls", () -> 3);
        registry.add("ecofy.gateway.resilience.circuit-breaker.failure-rate-threshold", () -> 50);
        registry.add("ecofy.gateway.resilience.circuit-breaker.wait-duration-in-open-state", () -> "5s");
        registry.add("ecofy.gateway.resilience.circuit-breaker.permitted-number-of-calls-in-half-open-state", () -> 2);
        registry.add("ecofy.gateway.resilience.circuit-breaker.time-limiter-timeout", () -> "3s");
    }

    private static int deadPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Test
    void circuitBreakerOpensAndReturnsControlledFallback() {
        boolean sawOpen = false;
        boolean sawUnavailable = false;

        for (int i = 0; i < 10 && !sawOpen; i++) {
            String body = client.get().uri("/api/v1/auth/ping")
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            if (body != null && body.contains("CIRCUIT_BREAKER_OPEN")) {
                sawOpen = true;
            } else if (body != null && body.contains("DOWNSTREAM_UNAVAILABLE")) {
                sawUnavailable = true;
            }
        }

        assertThat(sawUnavailable)
                .as("as primeiras falhas devem ser classificadas como indisponibilidade").isTrue();
        assertThat(sawOpen)
                .as("após o mínimo de falhas o circuit breaker deve abrir").isTrue();
    }

    @Test
    void circuitBreakerMetricsAreExposed() {
        // Gera pelo menos uma chamada pela instância cb-auth.
        client.get().uri("/api/v1/auth/ping").exchange();

        String prometheus = client.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(prometheus).contains("resilience4j_circuitbreaker");
        assertThat(prometheus).contains("cb-auth");
    }
}
