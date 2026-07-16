package br.com.ecofy.gateway.api_gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Profile dev: exposição ampliada para diagnóstico local.
 * O endpoint operacional 'gateway' deve ficar acessível (lista de rotas).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ActuatorExposureDevTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void gatewayEndpointIsExposedInDev() {
        webTestClient.get().uri("/actuator/gateway/routes")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void healthIsStillExposedInDev() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}
