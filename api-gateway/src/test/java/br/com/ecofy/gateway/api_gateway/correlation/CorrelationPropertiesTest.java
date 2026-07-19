package br.com.ecofy.gateway.api_gateway.correlation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes unitários das propriedades do correlation ID")
class CorrelationPropertiesTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("Deve retornar 128 como tamanho máximo padrão")
    void getMaxLength_novaInstancia_deveRetornarValorPadrao() {
        // Arrange
        CorrelationProperties properties = new CorrelationProperties();

        // Act
        int result = properties.getMaxLength();

        // Assert
        assertThat(result).isEqualTo(128);
    }

    @Test
    @DisplayName("Deve atualizar o tamanho máximo quando um valor válido for informado")
    void setMaxLength_valorValido_deveAtualizarTamanhoMaximo() {
        // Arrange
        CorrelationProperties properties = new CorrelationProperties();
        int expectedMaxLength = 64;

        // Act
        properties.setMaxLength(expectedMaxLength);

        // Assert
        assertThat(properties.getMaxLength()).isEqualTo(expectedMaxLength);
    }

    @Test
    @DisplayName("Deve aceitar o valor mínimo permitido sem gerar violações")
    void setMaxLength_valorMinimo_deveSerConsideradoValido() {
        // Arrange
        CorrelationProperties properties = new CorrelationProperties();
        properties.setMaxLength(8);

        // Act
        Set<ConstraintViolation<CorrelationProperties>> violations =
                validator.validate(properties);

        // Assert
        assertThat(properties.getMaxLength()).isEqualTo(8);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve gerar violação quando o tamanho máximo for menor que o permitido")
    void setMaxLength_valorAbaixoDoMinimo_deveGerarViolacao() {
        // Arrange
        CorrelationProperties properties = new CorrelationProperties();
        properties.setMaxLength(7);

        // Act
        Set<ConstraintViolation<CorrelationProperties>> violations =
                validator.validate(properties);

        // Assert
        assertThat(properties.getMaxLength()).isEqualTo(7);
        assertThat(violations)
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("maxLength");
                    assertThat(violation.getInvalidValue())
                            .isEqualTo(7);
                });
    }
}