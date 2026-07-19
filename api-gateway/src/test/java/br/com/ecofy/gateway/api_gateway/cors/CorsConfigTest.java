package br.com.ecofy.gateway.api_gateway.cors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes unitários da configuração CORS")
class CorsConfigTest {

    private static final String GATEWAY_URL =
            "https://gateway.ecofy.test/api/v1/users";

    private static final String ALLOWED_ORIGIN =
            "https://app.ecofy.test";

    private static final String BLOCKED_ORIGIN =
            "https://blocked.ecofy.test";

    private CorsConfig corsConfig;
    private CorsProperties properties;

    @BeforeEach
    void setUp() {
        corsConfig = new CorsConfig();

        properties = new CorsProperties();
        properties.setAllowedOrigins(List.of(ALLOWED_ORIGIN));
        properties.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));
        properties.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Correlation-Id"
        ));
        properties.setExposedHeaders(List.of("X-Correlation-Id"));
        properties.setAllowCredentials(false);
        properties.setMaxAge(3600);
    }

    @Test
    @DisplayName("Deve criar o filtro CORS com as propriedades configuradas")
    void corsWebFilter_propriedadesConfiguradas_deveCriarFiltro() {
        // Act
        CorsWebFilter result = corsConfig.corsWebFilter(properties);

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Deve permitir o preflight quando a origem, o método e os headers forem válidos")
    void corsWebFilter_preflightPermitido_deveRetornarHeadersCors() {
        // Arrange
        CorsWebFilter filter = corsConfig.corsWebFilter(properties);
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .options(GATEWAY_URL)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                        HttpMethod.GET.name()
                )
                .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                        "Authorization, Content-Type, X-Correlation-Id"
                )
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = currentExchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();

        assertThat(chainInvoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode())
                .isNotEqualTo(HttpStatus.FORBIDDEN);
        assertThat(responseHeaders.getAccessControlAllowOrigin())
                .isEqualTo(ALLOWED_ORIGIN);
        assertThat(responseHeaders.getAccessControlAllowMethods())
                .contains(HttpMethod.GET);
        assertThat(responseHeaders.getAccessControlAllowHeaders())
                .contains(
                        "Authorization",
                        "Content-Type",
                        "X-Correlation-Id"
                );
        assertThat(responseHeaders.getAccessControlMaxAge())
                .isEqualTo(3600);
    }

    @Test
    @DisplayName("Deve rejeitar o preflight quando a origem não estiver permitida")
    void corsWebFilter_origemBloqueada_deveRetornarForbidden() {
        // Arrange
        CorsWebFilter filter = corsConfig.corsWebFilter(properties);
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .options(GATEWAY_URL)
                .header(HttpHeaders.ORIGIN, BLOCKED_ORIGIN)
                .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                        HttpMethod.GET.name()
                )
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = currentExchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(chainInvoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse()
                .getHeaders()
                .getAccessControlAllowOrigin())
                .isNull();
    }

    @Test
    @DisplayName("Deve aplicar os headers CORS e continuar a cadeia em uma requisição permitida")
    void corsWebFilter_requisicaoPermitida_deveAplicarHeadersEContinuarCadeia() {
        // Arrange
        CorsWebFilter filter = corsConfig.corsWebFilter(properties);
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get(GATEWAY_URL)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = currentExchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();

        assertThat(chainInvoked).isTrue();
        assertThat(responseHeaders.getAccessControlAllowOrigin())
                .isEqualTo(ALLOWED_ORIGIN);
        assertThat(responseHeaders.getAccessControlExposeHeaders())
                .contains("X-Correlation-Id");
        assertThat(responseHeaders.getAccessControlAllowCredentials())
                .isFalse();
    }
}
