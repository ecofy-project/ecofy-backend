package br.com.ecofy.gateway.api_gateway.correlation;

import br.com.ecofy.gateway.api_gateway.support.GatewayHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes unitários da propagação do correlation ID no MDC")
class CorrelationIdThreadLocalAccessorTest {

    private final CorrelationIdThreadLocalAccessor accessor =
            new CorrelationIdThreadLocalAccessor();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve retornar a chave utilizada para propagar o correlation ID")
    void key_chaveConfigurada_deveRetornarChaveDoCorrelationId() {
        // Arrange
        String expectedKey = GatewayHeaders.CORRELATION_ID_CONTEXT_KEY;

        // Act
        Object result = accessor.key();

        // Assert
        assertThat(result).isEqualTo(expectedKey);
    }

    @Test
    @DisplayName("Deve retornar nulo quando o correlation ID não estiver no MDC")
    void getValue_correlationIdAusente_deveRetornarNulo() {
        // Arrange
        MDC.remove(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY);

        // Act
        String result = accessor.getValue();

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve retornar o correlation ID armazenado no MDC")
    void getValue_correlationIdPresente_deveRetornarValorArmazenado() {
        // Arrange
        String correlationId = "correlation-id-123";

        MDC.put(
                GatewayHeaders.CORRELATION_ID_CONTEXT_KEY,
                correlationId
        );

        // Act
        String result = accessor.getValue();

        // Assert
        assertThat(result).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve armazenar o correlation ID recebido no MDC")
    void setValue_valorInformado_deveArmazenarCorrelationIdNoMdc() {
        // Arrange
        String correlationId = "correlation-id-456";

        // Act
        accessor.setValue(correlationId);

        // Assert
        assertThat(MDC.get(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY))
                .isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve remover o correlation ID do MDC ao redefinir sem valor")
    void setValue_semArgumento_deveRemoverCorrelationIdDoMdc() {
        // Arrange
        MDC.put(
                GatewayHeaders.CORRELATION_ID_CONTEXT_KEY,
                "correlation-id-789"
        );

        // Act
        accessor.setValue();

        // Assert
        assertThat(MDC.get(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY))
                .isNull();
    }

    @Test
    @DisplayName("Deve remover o correlation ID do MDC durante a restauração")
    void restore_correlationIdPresente_deveRemoverValorDoMdc() {
        // Arrange
        MDC.put(
                GatewayHeaders.CORRELATION_ID_CONTEXT_KEY,
                "correlation-id-restore"
        );

        // Act
        accessor.restore();

        // Assert
        assertThat(MDC.get(GatewayHeaders.CORRELATION_ID_CONTEXT_KEY))
                .isNull();
    }
}