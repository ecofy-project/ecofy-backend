package br.com.ecofy.gateway.api_gateway.resilience;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes unitários das propriedades de resiliência")
class ResiliencePropertiesTest {

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
    @DisplayName("Deve retornar a mesma instância das propriedades de circuit breaker")
    void getCircuitBreaker_multiplasChamadas_deveRetornarMesmaInstancia() {
        // Arrange
        ResilienceProperties properties = new ResilienceProperties();

        // Act
        ResilienceProperties.CircuitBreaker firstResult =
                properties.getCircuitBreaker();
        ResilienceProperties.CircuitBreaker secondResult =
                properties.getCircuitBreaker();

        // Assert
        assertThat(firstResult)
                .isNotNull()
                .isSameAs(secondResult);
    }

    @Test
    @DisplayName("Deve retornar os valores padrão em uma nova instância")
    void getters_novaInstancia_deveRetornarValoresPadrao() {
        // Arrange
        ResilienceProperties properties = new ResilienceProperties();
        ResilienceProperties.CircuitBreaker circuitBreaker =
                properties.getCircuitBreaker();

        // Act e Assert
        assertThat(circuitBreaker.getSlidingWindowSize())
                .isEqualTo(20);
        assertThat(circuitBreaker.getMinimumNumberOfCalls())
                .isEqualTo(10);
        assertThat(circuitBreaker.getFailureRateThreshold())
                .isEqualTo(50f);
        assertThat(circuitBreaker.getWaitDurationInOpenState())
                .isEqualTo(Duration.ofSeconds(10));
        assertThat(circuitBreaker.getPermittedNumberOfCallsInHalfOpenState())
                .isEqualTo(3);
        assertThat(circuitBreaker.getTimeLimiterTimeout())
                .isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("Deve atualizar todas as propriedades quando novos valores forem informados")
    void setters_valoresCustomizados_deveAtualizarTodasAsPropriedades() {
        // Arrange
        ResilienceProperties.CircuitBreaker circuitBreaker =
                new ResilienceProperties.CircuitBreaker();

        Duration waitDuration = Duration.ofSeconds(15);
        Duration timeoutDuration = Duration.ofSeconds(12);

        // Act
        circuitBreaker.setSlidingWindowSize(30);
        circuitBreaker.setMinimumNumberOfCalls(12);
        circuitBreaker.setFailureRateThreshold(37.5f);
        circuitBreaker.setWaitDurationInOpenState(waitDuration);
        circuitBreaker.setPermittedNumberOfCallsInHalfOpenState(4);
        circuitBreaker.setTimeLimiterTimeout(timeoutDuration);

        // Assert
        assertThat(circuitBreaker.getSlidingWindowSize())
                .isEqualTo(30);
        assertThat(circuitBreaker.getMinimumNumberOfCalls())
                .isEqualTo(12);
        assertThat(circuitBreaker.getFailureRateThreshold())
                .isEqualTo(37.5f);
        assertThat(circuitBreaker.getWaitDurationInOpenState())
                .isEqualTo(waitDuration);
        assertThat(circuitBreaker.getPermittedNumberOfCallsInHalfOpenState())
                .isEqualTo(4);
        assertThat(circuitBreaker.getTimeLimiterTimeout())
                .isEqualTo(timeoutDuration);
    }

    @Test
    @DisplayName("Deve aceitar os valores mínimos permitidos sem gerar violações")
    void validation_valoresMinimos_deveRetornarSemViolacoes() {
        // Arrange
        ResilienceProperties.CircuitBreaker circuitBreaker =
                new ResilienceProperties.CircuitBreaker();

        circuitBreaker.setSlidingWindowSize(1);
        circuitBreaker.setMinimumNumberOfCalls(1);
        circuitBreaker.setFailureRateThreshold(1f);
        circuitBreaker.setPermittedNumberOfCallsInHalfOpenState(1);

        // Act
        Set<ConstraintViolation<ResilienceProperties.CircuitBreaker>> violations =
                validator.validate(circuitBreaker);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve aceitar o limite máximo da taxa de falhas sem gerar violações")
    void validation_taxaDeFalhasNoLimiteMaximo_deveRetornarSemViolacoes() {
        // Arrange
        ResilienceProperties.CircuitBreaker circuitBreaker =
                new ResilienceProperties.CircuitBreaker();

        circuitBreaker.setFailureRateThreshold(100f);

        // Act
        Set<ConstraintViolation<ResilienceProperties.CircuitBreaker>> violations =
                validator.validate(circuitBreaker);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve gerar violações quando valores mínimos não forem respeitados")
    void validation_valoresAbaixoDoMinimo_deveGerarViolacoes() {
        // Arrange
        ResilienceProperties.CircuitBreaker circuitBreaker =
                new ResilienceProperties.CircuitBreaker();

        circuitBreaker.setSlidingWindowSize(0);
        circuitBreaker.setMinimumNumberOfCalls(0);
        circuitBreaker.setFailureRateThreshold(0f);
        circuitBreaker.setPermittedNumberOfCallsInHalfOpenState(0);

        // Act
        Set<ConstraintViolation<ResilienceProperties.CircuitBreaker>> violations =
                validator.validate(circuitBreaker);

        // Assert
        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString()
                )
                .containsExactlyInAnyOrder(
                        "slidingWindowSize",
                        "minimumNumberOfCalls",
                        "failureRateThreshold",
                        "permittedNumberOfCallsInHalfOpenState"
                );
    }

    @Test
    @DisplayName("Deve gerar violação quando a taxa de falhas exceder o limite máximo")
    void validation_taxaDeFalhasAcimaDoMaximo_deveGerarViolacao() {
        // Arrange
        ResilienceProperties.CircuitBreaker circuitBreaker =
                new ResilienceProperties.CircuitBreaker();

        circuitBreaker.setFailureRateThreshold(100.1f);

        // Act
        Set<ConstraintViolation<ResilienceProperties.CircuitBreaker>> violations =
                validator.validate(circuitBreaker);

        // Assert
        assertThat(violations)
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("failureRateThreshold");
                    assertThat(violation.getInvalidValue())
                            .isEqualTo(100.1f);
                });
    }

    @Test
    @DisplayName("Deve aceitar durações nulas porque não existe validação interna")
    void setters_duracoesNulas_deveManterValoresNulos() {
        // Arrange
        ResilienceProperties.CircuitBreaker circuitBreaker =
                new ResilienceProperties.CircuitBreaker();

        // Act
        circuitBreaker.setWaitDurationInOpenState(null);
        circuitBreaker.setTimeLimiterTimeout(null);

        // Assert
        assertThat(circuitBreaker.getWaitDurationInOpenState())
                .isNull();
        assertThat(circuitBreaker.getTimeLimiterTimeout())
                .isNull();
        assertThat(validator.validate(circuitBreaker))
                .isEmpty();
    }
}
