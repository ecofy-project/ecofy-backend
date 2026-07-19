package br.com.ecofy.gateway.api_gateway.error;

import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do tratamento global de erros")
class GlobalErrorWebExceptionHandlerTest {

    private static final String CORRELATION_ID = "correlation-id-123";
    private static final String REQUEST_PATH = "/api/v1/users";

    @Mock
    private ApiErrorResponseWriter writer;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private RequestPath requestPath;

    private GlobalErrorWebExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalErrorWebExceptionHandler(writer);
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a resposta já estiver comprometida")
    void handle_respostaComprometida_devePropagarExcecao() {
        // Arrange
        RuntimeException exception = new RuntimeException(
                "Resposta já comprometida"
        );

        when(exchange.getResponse()).thenReturn(response);
        when(response.isCommitted()).thenReturn(true);

        // Act
        Mono<Void> result = handler.handle(exchange, exception);

        // Assert
        StepVerifier.create(result)
                .expectErrorSatisfies(error ->
                        assertThat(error).isSameAs(exception)
                )
                .verify();

        verifyNoInteractions(writer);
    }

    @Test
    @DisplayName("Deve retornar erro de circuit breaker quando o circuito estiver aberto")
    void handle_circuitBreakerAberto_deveRetornarErroCorrespondente() {
        // Arrange
        configurarExchangeNaoComprometido();

        CallNotPermittedException exception =
                mock(CallNotPermittedException.class);

        // Act
        ApiErrorResponse result = executarECapturarResposta(exception);

        // Assert
        assertErrorResponse(
                result,
                GatewayErrorCode.CIRCUIT_BREAKER_OPEN
        );
    }

    @Test
    @DisplayName("Deve retornar gateway timeout quando ocorrer uma exceção de timeout")
    void handle_timeout_deveRetornarGatewayTimeout() {
        // Arrange
        configurarExchangeNaoComprometido();

        TimeoutException exception = new TimeoutException(
                "Tempo limite excedido"
        );

        // Act
        ApiErrorResponse result = executarECapturarResposta(exception);

        // Assert
        assertErrorResponse(
                result,
                GatewayErrorCode.GATEWAY_TIMEOUT
        );
    }

    @Test
    @DisplayName("Deve localizar uma indisponibilidade downstream na cadeia de causas")
    void handle_connectExceptionAninhada_deveRetornarDownstreamUnavailable() {
        // Arrange
        configurarExchangeNaoComprometido();

        ConnectException cause = new ConnectException(
                "Conexão recusada"
        );
        RuntimeException exception = new RuntimeException(
                "Falha ao acessar o serviço",
                cause
        );

        // Act
        ApiErrorResponse result = executarECapturarResposta(exception);

        // Assert
        assertErrorResponse(
                result,
                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE
        );
    }

    @ParameterizedTest(name = "Status {0} deve produzir {1}")
    @DisplayName("Deve converter o status HTTP no código de erro correspondente")
    @CsvSource({
            "404, ROUTE_NOT_FOUND",
            "405, METHOD_NOT_ALLOWED",
            "503, DOWNSTREAM_UNAVAILABLE",
            "504, GATEWAY_TIMEOUT",
            "400, INVALID_REQUEST",
            "422, INVALID_REQUEST",
            "500, INTERNAL_GATEWAY_ERROR"
    })
    void handle_responseStatusException_deveRetornarCodigoCorrespondente(
            int status,
            GatewayErrorCode expectedCode
    ) {
        // Arrange
        configurarExchangeNaoComprometido();

        ResponseStatusException exception = new ResponseStatusException(
                HttpStatusCode.valueOf(status)
        );

        // Act
        ApiErrorResponse result = executarECapturarResposta(exception);

        // Assert
        assertErrorResponse(result, expectedCode);
    }

    @Test
    @DisplayName("Deve retornar erro interno quando a exceção não possuir classificação conhecida")
    void handle_excecaoDesconhecida_deveRetornarErroInterno() {
        // Arrange
        configurarExchangeNaoComprometido();

        IllegalStateException exception = new IllegalStateException(
                "Erro inesperado"
        );

        // Act
        ApiErrorResponse result = executarECapturarResposta(exception);

        // Assert
        assertErrorResponse(
                result,
                GatewayErrorCode.INTERNAL_GATEWAY_ERROR
        );
    }

    @Test
    @DisplayName("Deve tratar uma mensagem nula sem interromper a resposta de erro")
    void handle_excecaoComMensagemNula_deveRetornarErroInterno() {
        // Arrange
        configurarExchangeNaoComprometido();

        RuntimeException exception = new RuntimeException((String) null);

        // Act
        ApiErrorResponse result = executarECapturarResposta(exception);

        // Assert
        assertErrorResponse(
                result,
                GatewayErrorCode.INTERNAL_GATEWAY_ERROR
        );
    }

    @Test
    @DisplayName("Deve sanitizar quebras de linha sem interromper o tratamento")
    void handle_mensagemComQuebrasDeLinha_deveRetornarErroInterno() {
        // Arrange
        configurarExchangeNaoComprometido();

        RuntimeException exception = new RuntimeException(
                "Erro técnico\ncom quebra\r\nde linha"
        );

        // Act
        ApiErrorResponse result = executarECapturarResposta(exception);

        // Assert
        assertErrorResponse(
                result,
                GatewayErrorCode.INTERNAL_GATEWAY_ERROR
        );
    }

    @Test
    @DisplayName("Deve retornar menos dois como ordem de execução")
    void getOrder_chamadaRealizada_deveRetornarMenosDois() {
        // Act
        int result = handler.getOrder();

        // Assert
        assertThat(result).isEqualTo(-2);
        assertThat(result).isGreaterThan(Ordered.HIGHEST_PRECEDENCE);
    }

    private void configurarExchangeNaoComprometido() {
        when(exchange.getResponse()).thenReturn(response);
        when(response.isCommitted()).thenReturn(false);

        when(exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR))
                .thenReturn(CORRELATION_ID);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn(REQUEST_PATH);
        when(request.getMethod()).thenReturn(HttpMethod.GET);

        when(writer.write(eq(response), any(ApiErrorResponse.class)))
                .thenReturn(Mono.empty());
    }

    private ApiErrorResponse executarECapturarResposta(Throwable exception) {
        ArgumentCaptor<ApiErrorResponse> responseCaptor =
                ArgumentCaptor.forClass(ApiErrorResponse.class);

        Mono<Void> result = handler.handle(exchange, exception);

        StepVerifier.create(result)
                .verifyComplete();

        verify(writer).write(
                eq(response),
                responseCaptor.capture()
        );

        return responseCaptor.getValue();
    }

    private void assertErrorResponse(
            ApiErrorResponse responseBody,
            GatewayErrorCode expectedCode
    ) {
        assertThat(responseBody.status())
                .isEqualTo(expectedCode.status().value());
        assertThat(responseBody.errorCode())
                .isEqualTo(expectedCode.name());
        assertThat(responseBody.message())
                .isEqualTo(expectedCode.defaultMessage());
        assertThat(responseBody.path())
                .isEqualTo(REQUEST_PATH);
        assertThat(responseBody.traceId())
                .isEqualTo(CORRELATION_ID);
        assertThat(responseBody.details())
                .isEmpty();
        assertThat(responseBody.timestamp())
                .isNotNull();
    }
}
