package br.com.ecofy.gateway.api_gateway.correlation;

import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários da recuperação do correlation ID")
class CorrelationIdSupportTest {

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Test
    @DisplayName("Deve retornar o correlation ID armazenado no atributo do exchange")
    void resolve_atributoValido_deveRetornarCorrelationIdDoAtributo() {
        // Arrange
        String correlationId = "correlation-id-123";

        when(exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR))
                .thenReturn(correlationId);

        // Act
        String result = CorrelationIdSupport.resolve(exchange);

        // Assert
        assertThat(result).isEqualTo(correlationId);

        verify(exchange, never()).getRequest();
    }

    @Test
    @DisplayName("Deve retornar o correlation ID do header quando o atributo estiver ausente")
    void resolve_atributoAusenteHeaderValido_deveRetornarCorrelationIdDoHeader() {
        // Arrange
        String correlationId = "correlation-id-456";
        HttpHeaders headers = new HttpHeaders();
        headers.set(GatewayHeaders.CORRELATION_ID, correlationId);

        when(exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR))
                .thenReturn(null);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);

        // Act
        String result = CorrelationIdSupport.resolve(exchange);

        // Assert
        assertThat(result).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve retornar o correlation ID do header quando o atributo estiver vazio")
    void resolve_atributoVazioHeaderValido_deveRetornarCorrelationIdDoHeader() {
        // Arrange
        String correlationId = "correlation-id-789";
        HttpHeaders headers = new HttpHeaders();
        headers.set(GatewayHeaders.CORRELATION_ID, correlationId);

        when(exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR))
                .thenReturn("   ");
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);

        // Act
        String result = CorrelationIdSupport.resolve(exchange);

        // Assert
        assertThat(result).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve consultar o header quando o atributo não for uma String")
    void resolve_atributoComTipoInvalidoHeaderValido_deveRetornarCorrelationIdDoHeader() {
        // Arrange
        String correlationId = "correlation-id-header";
        HttpHeaders headers = new HttpHeaders();
        headers.set(GatewayHeaders.CORRELATION_ID, correlationId);

        when(exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR))
                .thenReturn(123);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);

        // Act
        String result = CorrelationIdSupport.resolve(exchange);

        // Assert
        assertThat(result).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve retornar unknown quando o atributo e o header estiverem ausentes")
    void resolve_atributoEHeaderAusentes_deveRetornarUnknown() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();

        when(exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR))
                .thenReturn(null);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);

        // Act
        String result = CorrelationIdSupport.resolve(exchange);

        // Assert
        assertThat(result).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Deve retornar unknown quando o atributo e o header estiverem vazios")
    void resolve_atributoEHeaderVazios_deveRetornarUnknown() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set(GatewayHeaders.CORRELATION_ID, "   ");

        when(exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR))
                .thenReturn("");
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);

        // Act
        String result = CorrelationIdSupport.resolve(exchange);

        // Assert
        assertThat(result).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Deve manter o construtor privado para impedir instanciação direta")
    void constructor_construtorPrivado_devePermitirCoberturaPorReflexao() throws Exception {
        // Arrange
        Constructor<CorrelationIdSupport> constructor =
                CorrelationIdSupport.class.getDeclaredConstructor();

        // Act
        constructor.setAccessible(true);
        CorrelationIdSupport instance = constructor.newInstance();

        // Assert
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        assertThat(instance).isNotNull();
    }
}