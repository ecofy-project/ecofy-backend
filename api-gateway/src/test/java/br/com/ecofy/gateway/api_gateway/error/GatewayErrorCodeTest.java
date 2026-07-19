package br.com.ecofy.gateway.api_gateway.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes unitários dos códigos de erro do Gateway")
class GatewayErrorCodeTest {

    @ParameterizedTest(name = "{0} deve utilizar o status {1}")
    @DisplayName("Deve retornar o status e a mensagem configurados para cada código")
    @CsvSource(
            delimiter = '|',
            textBlock = """
                    GATEWAY_TIMEOUT          | 504 | O serviço solicitado não respondeu dentro do tempo esperado.
                    DOWNSTREAM_UNAVAILABLE   | 503 | O serviço solicitado está temporariamente indisponível.
                    CIRCUIT_BREAKER_OPEN      | 503 | O serviço solicitado está temporariamente indisponível.
                    ROUTE_NOT_FOUND           | 404 | O recurso solicitado não foi encontrado.
                    METHOD_NOT_ALLOWED        | 405 | Método HTTP não suportado para este recurso.
                    INVALID_REQUEST           | 400 | A requisição contém dados inválidos.
                    INTERNAL_GATEWAY_ERROR    | 500 | Erro interno ao processar a requisição.
                    """
    )
    void accessors_codigoConfigurado_deveRetornarStatusEMensagemEsperados(
            String codeName,
            int expectedStatus,
            String expectedMessage
    ) {
        // Arrange
        GatewayErrorCode code = GatewayErrorCode.valueOf(codeName);

        // Act
        HttpStatus status = code.status();
        String message = code.defaultMessage();

        // Assert
        assertThat(status.value()).isEqualTo(expectedStatus);
        assertThat(message).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Deve retornar todos os códigos de erro declarados")
    void values_chamadaRealizada_deveRetornarTodosOsCodigos() {
        // Act
        GatewayErrorCode[] result = GatewayErrorCode.values();

        // Assert
        assertThat(result).containsExactly(
                GatewayErrorCode.GATEWAY_TIMEOUT,
                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE,
                GatewayErrorCode.CIRCUIT_BREAKER_OPEN,
                GatewayErrorCode.ROUTE_NOT_FOUND,
                GatewayErrorCode.METHOD_NOT_ALLOWED,
                GatewayErrorCode.INVALID_REQUEST,
                GatewayErrorCode.INTERNAL_GATEWAY_ERROR
        );
    }

    @Test
    @DisplayName("Deve retornar o código correspondente quando o nome for válido")
    void valueOf_nomeValido_deveRetornarCodigoCorrespondente() {
        // Act
        GatewayErrorCode result = GatewayErrorCode.valueOf(
                "GATEWAY_TIMEOUT"
        );

        // Assert
        assertThat(result).isEqualTo(GatewayErrorCode.GATEWAY_TIMEOUT);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome do código não existir")
    void valueOf_nomeInexistente_deveLancarIllegalArgumentException() {
        // Act e Assert
        assertThatThrownBy(() -> GatewayErrorCode.valueOf("UNKNOWN_ERROR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome do código for nulo")
    void valueOf_nomeNulo_deveLancarNullPointerException() {
        // Act e Assert
        assertThatThrownBy(() -> GatewayErrorCode.valueOf(null))
                .isInstanceOf(NullPointerException.class);
    }
}