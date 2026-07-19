package br.com.ecofy.gateway.api_gateway.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes unitários dos detalhes da resposta de erro")
class ApiErrorDetailTest {

    @Test
    @DisplayName("Deve armazenar e retornar todos os valores informados")
    void constructor_valoresInformados_deveRetornarTodosOsCampos() {
        // Arrange
        String field = "email";
        String code = "INVALID_EMAIL";
        String message = "O e-mail informado é inválido.";

        // Act
        ApiErrorDetail detail = new ApiErrorDetail(
                field,
                code,
                message
        );

        // Assert
        assertThat(detail.field()).isEqualTo(field);
        assertThat(detail.code()).isEqualTo(code);
        assertThat(detail.message()).isEqualTo(message);
    }

    @Test
    @DisplayName("Deve preservar valores nulos porque o record não possui validação interna")
    void constructor_valoresNulos_deveManterCamposNulos() {
        // Act
        ApiErrorDetail detail = new ApiErrorDetail(
                null,
                null,
                null
        );

        // Assert
        assertThat(detail.field()).isNull();
        assertThat(detail.code()).isNull();
        assertThat(detail.message()).isNull();
    }

    @Test
    @DisplayName("Deve considerar iguais os detalhes com os mesmos valores")
    void equals_valoresIguais_deveRetornarVerdadeiro() {
        // Arrange
        ApiErrorDetail first = new ApiErrorDetail(
                "email",
                "INVALID_EMAIL",
                "O e-mail informado é inválido."
        );

        ApiErrorDetail second = new ApiErrorDetail(
                "email",
                "INVALID_EMAIL",
                "O e-mail informado é inválido."
        );

        // Act
        boolean result = first.equals(second);

        // Assert
        assertThat(result).isTrue();
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("Deve considerar diferentes os detalhes com valores distintos")
    void equals_valoresDiferentes_deveRetornarFalso() {
        // Arrange
        ApiErrorDetail first = new ApiErrorDetail(
                "email",
                "INVALID_EMAIL",
                "O e-mail informado é inválido."
        );

        ApiErrorDetail second = new ApiErrorDetail(
                "name",
                "INVALID_NAME",
                "O nome informado é inválido."
        );

        // Act
        boolean result = first.equals(second);

        // Assert
        assertThat(result).isFalse();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Deve gerar o mesmo hash code para detalhes com valores iguais")
    void hashCode_valoresIguais_deveRetornarMesmoHashCode() {
        // Arrange
        ApiErrorDetail first = new ApiErrorDetail(
                "email",
                "INVALID_EMAIL",
                "O e-mail informado é inválido."
        );

        ApiErrorDetail second = new ApiErrorDetail(
                "email",
                "INVALID_EMAIL",
                "O e-mail informado é inválido."
        );

        // Act
        int firstHashCode = first.hashCode();
        int secondHashCode = second.hashCode();

        // Assert
        assertThat(firstHashCode).isEqualTo(secondHashCode);
    }

    @Test
    @DisplayName("Deve representar todos os campos ao converter o detalhe em texto")
    void toString_detalhePreenchido_deveConterTodosOsCampos() {
        // Arrange
        ApiErrorDetail detail = new ApiErrorDetail(
                "email",
                "INVALID_EMAIL",
                "O e-mail informado é inválido."
        );

        // Act
        String result = detail.toString();

        // Assert
        assertThat(result)
                .contains("field=email")
                .contains("code=INVALID_EMAIL")
                .contains("message=O e-mail informado é inválido.");
    }
}
