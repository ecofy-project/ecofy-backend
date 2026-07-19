package br.com.ecofy.gateway.api_gateway.correlation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Testes unitários da validação e geração de correlation IDs")
class CorrelationIdValidatorTest {

    private static final int MAX_LENGTH = 36;

    private CorrelationIdValidator validator;

    @BeforeEach
    void setUp() {
        CorrelationProperties properties = new CorrelationProperties();
        properties.setMaxLength(MAX_LENGTH);

        validator = new CorrelationIdValidator(properties);
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando o valor for nulo")
    void isValid_valorNulo_deveRetornarFalso() {
        // Arrange
        String candidate = null;

        // Act
        boolean result = validator.isValid(candidate);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando o valor estiver vazio")
    void isValid_valorVazio_deveRetornarFalso() {
        // Arrange
        String candidate = "";

        // Act
        boolean result = validator.isValid(candidate);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando o valor contiver somente espaços")
    void isValid_valorEmBranco_deveRetornarFalso() {
        // Arrange
        String candidate = "   ";

        // Act
        boolean result = validator.isValid(candidate);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve aceitar o correlation ID quando o tamanho atingir o limite permitido")
    void isValid_tamanhoIgualAoLimite_deveRetornarVerdadeiro() {
        // Arrange
        String candidate = "a".repeat(MAX_LENGTH);

        // Act
        boolean result = validator.isValid(candidate);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando o tamanho exceder o limite permitido")
    void isValid_tamanhoMaiorQueLimite_deveRetornarFalso() {
        // Arrange
        String candidate = "a".repeat(MAX_LENGTH + 1);

        // Act
        boolean result = validator.isValid(candidate);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve aceitar o correlation ID quando utilizar todos os caracteres permitidos")
    void isValid_caracteresPermitidos_deveRetornarVerdadeiro() {
        // Arrange
        String candidate = "ABCabc123._-";

        // Act
        boolean result = validator.isValid(candidate);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve desconsiderar espaços externos ao validar o correlation ID")
    void isValid_espacosExternos_deveRetornarVerdadeiro() {
        // Arrange
        String candidate = "  correlation-id_123.abc  ";

        // Act
        boolean result = validator.isValid(candidate);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando contiver espaços internos")
    void isValid_espacosInternos_deveRetornarFalso() {
        // Arrange
        String candidate = "correlation id";

        // Act
        boolean result = validator.isValid(candidate);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando contiver caracteres não permitidos")
    void isValid_caracteresNaoPermitidos_deveRetornarFalso() {
        // Arrange
        String candidate = "correlation@id#123";

        // Act
        boolean result = validator.isValid(candidate);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve gerar um correlation ID no formato UUID")
    void generate_chamadaRealizada_deveRetornarUuidValido() {
        // Act
        String result = validator.generate();

        // Assert
        assertThat(result).isNotBlank();
        assertThatCode(() -> UUID.fromString(result))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve gerar um correlation ID diferente a cada chamada")
    void generate_multiplasChamadas_deveRetornarValoresDiferentes() {
        // Act
        String firstResult = validator.generate();
        String secondResult = validator.generate();

        // Assert
        assertThat(firstResult).isNotEqualTo(secondResult);
    }
}