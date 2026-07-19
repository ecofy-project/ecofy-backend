package br.com.ecofy.gateway.api_gateway.correlation;

import br.com.ecofy.gateway.api_gateway.metrics.GatewayMetrics;
import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários da propagação do correlation ID")
class CorrelationIdWebFilterTest {

    @Mock
    private CorrelationIdValidator validator;

    @Mock
    private GatewayMetrics metrics;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private WebFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpRequest.Builder requestBuilder;

    @Mock
    private ServerHttpRequest mutatedRequest;

    @Mock
    private ServerWebExchange.Builder exchangeBuilder;

    @Mock
    private ServerWebExchange mutatedExchange;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private RequestPath requestPath;

    private CorrelationIdWebFilter filter;
    private HttpHeaders requestHeaders;
    private HttpHeaders mutatedRequestHeaders;
    private HttpHeaders responseHeaders;
    private Map<String, Object> exchangeAttributes;
    private AtomicReference<String> contextCorrelationId;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdWebFilter(validator, metrics);

        requestHeaders = new HttpHeaders();
        mutatedRequestHeaders = new HttpHeaders();
        responseHeaders = new HttpHeaders();
        exchangeAttributes = new HashMap<>();
        contextCorrelationId = new AtomicReference<>();
    }

    @Test
    @DisplayName("Deve gerar e propagar um correlation ID quando o header estiver ausente")
    void filter_headerAusente_deveGerarEPropagarCorrelationId() {
        // Arrange
        configurarFluxoDoFiltro();

        String generatedCorrelationId = "generated-correlation-id";

        when(validator.generate()).thenReturn(generatedCorrelationId);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertCorrelationIdPropagated(generatedCorrelationId);

        verify(metrics).correlationIdMissing();
        verify(metrics, never()).correlationIdInvalidReplaced();
        verify(validator).generate();
        verify(validator, never()).isValid(any());
        verify(chain).filter(mutatedExchange);
    }

    @Test
    @DisplayName("Deve preservar e propagar o correlation ID quando o header for válido")
    void filter_headerValido_devePreservarEPropagarCorrelationId() {
        // Arrange
        configurarFluxoDoFiltro();

        String receivedCorrelationId = "  valid-correlation-id  ";
        String expectedCorrelationId = "valid-correlation-id";

        requestHeaders.set(
                GatewayHeaders.CORRELATION_ID,
                receivedCorrelationId
        );

        when(validator.isValid(receivedCorrelationId)).thenReturn(true);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertCorrelationIdPropagated(expectedCorrelationId);

        verify(validator).isValid(receivedCorrelationId);
        verify(validator, never()).generate();
        verifyNoInteractions(metrics);
        verify(chain).filter(mutatedExchange);
    }

    @Test
    @DisplayName("Deve substituir e propagar o correlation ID quando o header for inválido")
    void filter_headerInvalido_deveSubstituirEPropagarCorrelationId() {
        // Arrange
        configurarFluxoDoFiltro();

        String invalidCorrelationId = "invalid correlation id";
        String generatedCorrelationId = "replacement-correlation-id";

        requestHeaders.set(
                GatewayHeaders.CORRELATION_ID,
                invalidCorrelationId
        );

        when(validator.isValid(invalidCorrelationId)).thenReturn(false);
        when(validator.generate()).thenReturn(generatedCorrelationId);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertCorrelationIdPropagated(generatedCorrelationId);

        verify(validator).isValid(invalidCorrelationId);
        verify(validator).generate();
        verify(metrics).correlationIdInvalidReplaced();
        verify(metrics, never()).correlationIdMissing();
        verify(chain).filter(mutatedExchange);
    }

    @Test
    @DisplayName("Deve executar o processamento somente após a inscrição reativa")
    void filter_semInscricao_deveAdiarProcessamentoDaCadeia() {
        // Arrange
        configurarFluxoDoFiltro();

        String generatedCorrelationId = "deferred-correlation-id";

        when(validator.generate()).thenReturn(generatedCorrelationId);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        assertThat(result).isNotNull();
        assertThat(contextCorrelationId.get()).isNull();

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(contextCorrelationId.get())
                .isEqualTo(generatedCorrelationId);
    }

    @Test
    @DisplayName("Deve retornar a maior precedência para executar antes dos demais filtros")
    void getOrder_chamadaRealizada_deveRetornarMaiorPrecedencia() {
        // Act
        int result = filter.getOrder();

        // Assert
        assertThat(result).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    private void configurarFluxoDoFiltro() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(requestHeaders);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn("/api/v1/users");

        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.headers(any())).thenAnswer(invocation -> {
            Consumer<HttpHeaders> consumer = invocation.getArgument(0);
            consumer.accept(mutatedRequestHeaders);

            return requestBuilder;
        });
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(mutatedRequest)).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);

        when(mutatedExchange.getAttributes()).thenReturn(exchangeAttributes);
        when(mutatedExchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(responseHeaders);

        doAnswer(invocation -> {
            Supplier<? extends Mono<Void>> action = invocation.getArgument(0);

            StepVerifier.create(action.get())
                    .verifyComplete();

            return null;
        }).when(response).beforeCommit(any());

        when(chain.filter(mutatedExchange)).thenReturn(
                Mono.deferContextual(context -> {
                    contextCorrelationId.set(
                            context.get(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY)
                    );

                    return Mono.empty();
                })
        );
    }

    private void assertCorrelationIdPropagated(String expectedCorrelationId) {
        assertThat(mutatedRequestHeaders.getFirst(GatewayHeaders.CORRELATION_ID))
                .isEqualTo(expectedCorrelationId);

        assertThat(exchangeAttributes)
                .containsEntry(
                        GatewayHeaders.CORRELATION_ID_ATTR,
                        expectedCorrelationId
                );

        assertThat(responseHeaders.getFirst(GatewayHeaders.CORRELATION_ID))
                .isEqualTo(expectedCorrelationId);

        assertThat(contextCorrelationId.get())
                .isEqualTo(expectedCorrelationId);

        verify(response).beforeCommit(any());
    }
}
