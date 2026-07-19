package br.com.ecofy.gateway.api_gateway.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes unitários do contrato padronizado de erro")
class ApiErrorResponseTest {

    @Test
    @DisplayName("Deve armazenar e retornar todos os valores informados")
    void constructor_valoresInformados_deveRetornarTodosOsCampos() {
        // Arrange
        Instant timestamp = Instant.parse("2026-07-19T12:00:00Z");
        ApiErrorDetail detail = new ApiErrorDetail(
                "email",
                "INVALID_EMAIL",
                "O e-mail informado é inválido."
        );

        // Act
        ApiErrorResponse response = new ApiErrorResponse(
                timestamp,
                400,
                "INVALID_REQUEST",
                "A requisição contém dados inválidos.",
                "/api/v1/users",
                "trace-id-123",
                List.of(detail)
        );

        // Assert
        assertThat(response.timestamp()).isEqualTo(timestamp);
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo("INVALID_REQUEST");
        assertThat(response.message())
                .isEqualTo("A requisição contém dados inválidos.");
        assertThat(response.path()).isEqualTo("/api/v1/users");
        assertThat(response.traceId()).isEqualTo("trace-id-123");
        assertThat(response.details()).containsExactly(detail);
    }

    @Test
    @DisplayName("Deve converter a lista de detalhes nula em uma lista vazia")
    void constructor_detalhesNulos_deveRetornarListaVazia() {
        // Act
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.parse("2026-07-19T12:00:00Z"),
                500,
                "INTERNAL_GATEWAY_ERROR",
                "Erro interno ao processar a requisição.",
                "/api/v1/users",
                "trace-id-123",
                null
        );

        // Assert
        assertThat(response.details())
                .isNotNull()
                .isEmpty();
    }

    @Test
    @DisplayName("Deve criar uma cópia imutável da lista de detalhes")
    void constructor_listaMutavel_deveCriarCopiaImutavel() {
        // Arrange
        ApiErrorDetail detail = new ApiErrorDetail(
                "email",
                "INVALID_EMAIL",
                "O e-mail informado é inválido."
        );

        List<ApiErrorDetail> mutableDetails = new ArrayList<>();
        mutableDetails.add(detail);

        // Act
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.parse("2026-07-19T12:00:00Z"),
                400,
                "INVALID_REQUEST",
                "A requisição contém dados inválidos.",
                "/api/v1/users",
                "trace-id-123",
                mutableDetails
        );

        mutableDetails.clear();

        // Assert
        assertThat(response.details()).containsExactly(detail);

        assertThatThrownBy(() -> response.details().add(detail))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar uma lista de detalhes que contenha valor nulo")
    void constructor_listaComElementoNulo_deveLancarNullPointerException() {
        // Arrange
        List<ApiErrorDetail> detailsWithNull = new ArrayList<>();
        detailsWithNull.add(null);

        // Act e Assert
        assertThatThrownBy(() -> new ApiErrorResponse(
                Instant.parse("2026-07-19T12:00:00Z"),
                400,
                "INVALID_REQUEST",
                "A requisição contém dados inválidos.",
                "/api/v1/users",
                "trace-id-123",
                detailsWithNull
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve preservar campos nulos que não possuem validação interna")
    void constructor_camposOpcionaisNulos_deveManterValoresNulos() {
        // Act
        ApiErrorResponse response = new ApiErrorResponse(
                null,
                0,
                null,
                null,
                null,
                null,
                null
        );

        // Assert
        assertThat(response.timestamp()).isNull();
        assertThat(response.status()).isZero();
        assertThat(response.errorCode()).isNull();
        assertThat(response.message()).isNull();
        assertThat(response.path()).isNull();
        assertThat(response.traceId()).isNull();
        assertThat(response.details()).isEmpty();
    }

    @Test
    @DisplayName("Deve criar uma resposta usando o status e a mensagem segura do código informado")
    void of_codigoInformado_deveCriarRespostaPadronizada() {
        // Arrange
        GatewayErrorCode code = GatewayErrorCode.GATEWAY_TIMEOUT;
        String path = "/api/v1/insights";
        String traceId = "trace-id-timeout";
        Instant beforeCreation = Instant.now();

        // Act
        ApiErrorResponse response = ApiErrorResponse.of(
                code,
                path,
                traceId
        );

        Instant afterCreation = Instant.now();

        // Assert
        assertThat(response.timestamp())
                .isAfterOrEqualTo(beforeCreation)
                .isBeforeOrEqualTo(afterCreation);
        assertThat(response.status())
                .isEqualTo(code.status().value());
        assertThat(response.errorCode())
                .isEqualTo(code.name());
        assertThat(response.message())
                .isEqualTo(code.defaultMessage());
        assertThat(response.path()).isEqualTo(path);
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.details()).isEmpty();
    }

    @Test
    @DisplayName("Deve aceitar path e trace ID nulos ao criar a resposta pelo código")
    void of_pathETraceIdNulos_devePreservarValoresNulos() {
        // Arrange
        GatewayErrorCode code = GatewayErrorCode.INTERNAL_GATEWAY_ERROR;

        // Act
        ApiErrorResponse response = ApiErrorResponse.of(
                code,
                null,
                null
        );

        // Assert
        assertThat(response.status())
                .isEqualTo(code.status().value());
        assertThat(response.errorCode())
                .isEqualTo(code.name());
        assertThat(response.message())
                .isEqualTo(code.defaultMessage());
        assertThat(response.path()).isNull();
        assertThat(response.traceId()).isNull();
        assertThat(response.details()).isEmpty();
    }

    @Test
    @DisplayName("Deve considerar iguais as respostas que possuem os mesmos valores")
    void equals_valoresIguais_deveRetornarVerdadeiro() {
        // Arrange
        Instant timestamp = Instant.parse("2026-07-19T12:00:00Z");

        ApiErrorResponse first = createResponse(timestamp, "trace-id-123");
        ApiErrorResponse second = createResponse(timestamp, "trace-id-123");

        // Act
        boolean result = first.equals(second);

        // Assert
        assertThat(result).isTrue();
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("Deve considerar diferentes as respostas que possuem valores distintos")
    void equals_valoresDiferentes_deveRetornarFalso() {
        // Arrange
        Instant timestamp = Instant.parse("2026-07-19T12:00:00Z");

        ApiErrorResponse first = createResponse(timestamp, "trace-id-123");
        ApiErrorResponse second = createResponse(timestamp, "trace-id-456");

        // Act
        boolean result = first.equals(second);

        // Assert
        assertThat(result).isFalse();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Deve gerar o mesmo hash code para respostas com os mesmos valores")
    void hashCode_valoresIguais_deveRetornarMesmoHashCode() {
        // Arrange
        Instant timestamp = Instant.parse("2026-07-19T12:00:00Z");

        ApiErrorResponse first = createResponse(timestamp, "trace-id-123");
        ApiErrorResponse second = createResponse(timestamp, "trace-id-123");

        // Act
        int firstHashCode = first.hashCode();
        int secondHashCode = second.hashCode();

        // Assert
        assertThat(firstHashCode).isEqualTo(secondHashCode);
    }

    @Test
    @DisplayName("Deve representar os campos ao converter a resposta em texto")
    void toString_respostaPreenchida_deveConterCamposPrincipais() {
        // Arrange
        ApiErrorResponse response = createResponse(
                Instant.parse("2026-07-19T12:00:00Z"),
                "trace-id-123"
        );

        // Act
        String result = response.toString();

        // Assert
        assertThat(result)
                .contains("timestamp=2026-07-19T12:00:00Z")
                .contains("status=400")
                .contains("errorCode=INVALID_REQUEST")
                .contains("path=/api/v1/users")
                .contains("traceId=trace-id-123")
                .contains("details=[]");
    }

    private ApiErrorResponse createResponse(
            Instant timestamp,
            String traceId
    ) {
        return new ApiErrorResponse(
                timestamp,
                400,
                "INVALID_REQUEST",
                "A requisição contém dados inválidos.",
                "/api/v1/users",
                traceId,
                List.of()
        );
    }
}
