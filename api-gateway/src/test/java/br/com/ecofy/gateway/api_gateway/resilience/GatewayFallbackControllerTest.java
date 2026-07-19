package br.com.ecofy.gateway.api_gateway.resilience;

import br.com.ecofy.gateway.api_gateway.error.ApiErrorResponse;
import br.com.ecofy.gateway.api_gateway.error.GatewayErrorCode;
import br.com.ecofy.gateway.api_gateway.metrics.GatewayMetrics;
import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do fallback técnico do Gateway")
class GatewayFallbackControllerTest {

    private static final String CORRELATION_ID = "correlation-id-123";
    private static final String ORIGINAL_PATH = "/api/v1/users";
    private static final String CURRENT_PATH = "/__gateway/fallback";

    @Mock
    private GatewayMetrics metrics;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private RequestPath requestPath;

    private GatewayFallbackController controller;

    @BeforeEach
    void setUp() {
        controller = new GatewayFallbackController(metrics);
    }

    @Test
    @DisplayName("Deve retornar circuit breaker aberto e preservar o path de uma URI direta")
    void fallback_circuitBreakerAbertoEUriDireta_deveRetornarErroCorrespondente() {
        // Arrange
        CallNotPermittedException cause =
                mock(CallNotPermittedException.class);

        URI originalUri = URI.create(
                "https://gateway.ecofy.test" + ORIGINAL_PATH
        );

        configurarAtributos(cause, originalUri);

        // Act
        ResponseEntity<ApiErrorResponse> result = executarFallback();

        // Assert
        assertFallbackResponse(
                result,
                GatewayErrorCode.CIRCUIT_BREAKER_OPEN,
                ORIGINAL_PATH
        );
    }

    @Test
    @DisplayName("Deve retornar gateway timeout quando ocorrer o timeout do Gateway")
    void fallback_gatewayTimeout_deveRetornarErroCorrespondente() {
        // Arrange
        org.springframework.cloud.gateway.support.TimeoutException cause =
                mock(org.springframework.cloud.gateway.support.TimeoutException.class);

        URI originalUri = URI.create(
                "https://gateway.ecofy.test/api/v1/insights"
        );

        configurarAtributos(cause, originalUri);

        // Act
        ResponseEntity<ApiErrorResponse> result = executarFallback();

        // Assert
        assertFallbackResponse(
                result,
                GatewayErrorCode.GATEWAY_TIMEOUT,
                "/api/v1/insights"
        );
    }

    @Test
    @DisplayName("Deve retornar gateway timeout quando ocorrer um timeout concorrente")
    void fallback_timeoutConcorrente_deveRetornarErroCorrespondente() {
        // Arrange
        TimeoutException cause = new TimeoutException(
                "Tempo limite excedido"
        );

        URI originalUri = URI.create(
                "https://gateway.ecofy.test/api/v1/budgeting"
        );

        configurarAtributos(cause, Set.of(originalUri));

        // Act
        ResponseEntity<ApiErrorResponse> result = executarFallback();

        // Assert
        assertFallbackResponse(
                result,
                GatewayErrorCode.GATEWAY_TIMEOUT,
                "/api/v1/budgeting"
        );
    }

    @Test
    @DisplayName("Deve localizar uma indisponibilidade downstream na cadeia de causas")
    void fallback_connectExceptionAninhada_deveRetornarDownstreamUnavailable() {
        // Arrange
        ConnectException connectException = new ConnectException(
                "Conexão recusada"
        );

        RuntimeException cause = new RuntimeException(
                "Falha na chamada downstream",
                connectException
        );

        configurarAtributos(cause, null);
        configurarPathAtual();

        // Act
        ResponseEntity<ApiErrorResponse> result = executarFallback();

        // Assert
        assertFallbackResponse(
                result,
                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE,
                CURRENT_PATH
        );
    }

    @Test
    @DisplayName("Deve retornar indisponibilidade quando a causa não estiver disponível")
    void fallback_causaNula_deveRetornarDownstreamUnavailable() {
        // Arrange
        configurarAtributos(null, List.of());
        configurarPathAtual();

        // Act
        ResponseEntity<ApiErrorResponse> result = executarFallback();

        // Assert
        assertFallbackResponse(
                result,
                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE,
                CURRENT_PATH
        );
    }

    @ParameterizedTest(name = "Status {0} deve produzir {1}")
    @DisplayName("Deve converter o status HTTP no código de erro correspondente")
    @CsvSource({
            "504, GATEWAY_TIMEOUT",
            "404, ROUTE_NOT_FOUND",
            "405, METHOD_NOT_ALLOWED",
            "500, DOWNSTREAM_UNAVAILABLE",
            "503, DOWNSTREAM_UNAVAILABLE"
    })
    void fallback_responseStatusException_deveRetornarCodigoCorrespondente(
            int status,
            GatewayErrorCode expectedCode
    ) {
        // Arrange
        ResponseStatusException cause = new ResponseStatusException(
                HttpStatusCode.valueOf(status)
        );

        URI originalUri = URI.create(
                "https://gateway.ecofy.test" + ORIGINAL_PATH
        );

        configurarAtributos(cause, originalUri);

        // Act
        ResponseEntity<ApiErrorResponse> result = executarFallback();

        // Assert
        assertFallbackResponse(
                result,
                expectedCode,
                ORIGINAL_PATH
        );
    }

    @Test
    @DisplayName("Deve usar o path atual quando a coleção original estiver vazia")
    void fallback_colecaoOriginalVazia_deveUsarPathAtual() {
        // Arrange
        RuntimeException cause = new RuntimeException(
                "Falha não classificada"
        );

        configurarAtributos(cause, List.of());
        configurarPathAtual();

        // Act
        ResponseEntity<ApiErrorResponse> result = executarFallback();

        // Assert
        assertFallbackResponse(
                result,
                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE,
                CURRENT_PATH
        );
    }

    @Test
    @DisplayName("Deve usar o path atual quando o primeiro valor da coleção não for uma URI")
    void fallback_colecaoComValorInvalido_deveUsarPathAtual() {
        // Arrange
        RuntimeException cause = new RuntimeException(
                "Falha não classificada"
        );

        configurarAtributos(cause, List.of("valor-invalido"));
        configurarPathAtual();

        // Act
        ResponseEntity<ApiErrorResponse> result = executarFallback();

        // Assert
        assertFallbackResponse(
                result,
                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE,
                CURRENT_PATH
        );
    }

    @Test
    @DisplayName("Deve usar o path atual quando a URI original não possuir path")
    void fallback_uriOriginalSemPath_deveUsarPathAtual() {
        // Arrange
        RuntimeException cause = new RuntimeException(
                "Falha não classificada"
        );

        URI originalUri = URI.create("mailto:ecofy@example.com");

        configurarAtributos(cause, originalUri);
        configurarPathAtual();

        // Act
        ResponseEntity<ApiErrorResponse> result = executarFallback();

        // Assert
        assertFallbackResponse(
                result,
                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE,
                CURRENT_PATH
        );
    }

    private void configurarAtributos(
            Throwable cause,
            Object originalRequestUrl
    ) {
        when(exchange.getAttribute(
                ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR
        )).thenReturn(cause);

        when(exchange.getAttribute(
                GatewayHeaders.CORRELATION_ID_ATTR
        )).thenReturn(CORRELATION_ID);

        when(exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR
        )).thenReturn(originalRequestUrl);
    }

    private void configurarPathAtual() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn(CURRENT_PATH);
    }

    private ResponseEntity<ApiErrorResponse> executarFallback() {
        AtomicReference<ResponseEntity<ApiErrorResponse>> responseReference =
                new AtomicReference<>();

        Mono<ResponseEntity<ApiErrorResponse>> result =
                controller.fallback(exchange);

        StepVerifier.create(result)
                .assertNext(responseReference::set)
                .verifyComplete();

        return responseReference.get();
    }

    private void assertFallbackResponse(
            ResponseEntity<ApiErrorResponse> response,
            GatewayErrorCode expectedCode,
            String expectedPath
    ) {
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode())
                .isEqualTo(expectedCode.status());
        assertThat(response.getHeaders().getFirst(
                GatewayHeaders.CORRELATION_ID
        )).isEqualTo(CORRELATION_ID);

        ApiErrorResponse body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.status())
                .isEqualTo(expectedCode.status().value());
        assertThat(body.errorCode())
                .isEqualTo(expectedCode.name());
        assertThat(body.message())
                .isEqualTo(expectedCode.defaultMessage());
        assertThat(body.path())
                .isEqualTo(expectedPath);
        assertThat(body.traceId())
                .isEqualTo(CORRELATION_ID);
        assertThat(body.details())
                .isEmpty();
        assertThat(body.timestamp())
                .isNotNull();

        verify(metrics).fallback(expectedCode.name());
    }
}
