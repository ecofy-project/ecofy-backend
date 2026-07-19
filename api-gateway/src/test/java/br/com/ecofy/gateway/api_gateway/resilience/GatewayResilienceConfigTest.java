package br.com.ecofy.gateway.api_gateway.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;

import java.time.Duration;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários da configuração de resiliência do Gateway")
class GatewayResilienceConfigTest {

    @Mock
    private ReactiveResilience4JCircuitBreakerFactory factory;

    @Test
    @DisplayName("Deve criar o customizador com as configurações de circuit breaker e timeout")
    void defaultCircuitBreakerCustomizer_propriedadesConfiguradas_deveCriarConfiguracaoEsperada() {
        // Arrange
        GatewayResilienceConfig gatewayResilienceConfig =
                new GatewayResilienceConfig();

        ResilienceProperties properties = createProperties();

        // Act
        Customizer<ReactiveResilience4JCircuitBreakerFactory> customizer =
                gatewayResilienceConfig.defaultCircuitBreakerCustomizer(
                        properties
                );

        customizer.customize(factory);

        // Assert
        ArgumentCaptor<Function<
                String,
                Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration
                >> configurationCaptor = createConfigurationCaptor();

        verify(factory).configureDefault(
                configurationCaptor.capture()
        );

        Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration configuration =
                configurationCaptor.getValue().apply("cb-users");

        CircuitBreakerConfig circuitBreakerConfig =
                configuration.getCircuitBreakerConfig();

        TimeLimiterConfig timeLimiterConfig =
                configuration.getTimeLimiterConfig();

        assertThat(circuitBreakerConfig.getSlidingWindowType())
                .isEqualTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        assertThat(circuitBreakerConfig.getSlidingWindowSize())
                .isEqualTo(30);
        assertThat(circuitBreakerConfig.getMinimumNumberOfCalls())
                .isEqualTo(12);
        assertThat(circuitBreakerConfig.getFailureRateThreshold())
                .isEqualTo(37.5f);
        assertThat(circuitBreakerConfig
                .getPermittedNumberOfCallsInHalfOpenState())
                .isEqualTo(4);
        assertThat(circuitBreakerConfig
                .getWaitIntervalFunctionInOpenState()
                .apply(1))
                .isEqualTo(Duration.ofSeconds(15).toMillis());

        assertThat(timeLimiterConfig.getTimeoutDuration())
                .isEqualTo(Duration.ofSeconds(9));
    }

    @Test
    @DisplayName("Deve retornar um customizador sem configurar a fábrica antecipadamente")
    void defaultCircuitBreakerCustomizer_chamadaRealizada_deveRetornarCustomizador() {
        // Arrange
        GatewayResilienceConfig gatewayResilienceConfig =
                new GatewayResilienceConfig();

        ResilienceProperties properties = createProperties();

        // Act
        Customizer<ReactiveResilience4JCircuitBreakerFactory> result =
                gatewayResilienceConfig.defaultCircuitBreakerCustomizer(
                        properties
                );

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção quando as propriedades forem nulas")
    void defaultCircuitBreakerCustomizer_propriedadesNulas_deveLancarNullPointerException() {
        // Arrange
        GatewayResilienceConfig gatewayResilienceConfig =
                new GatewayResilienceConfig();

        // Act e Assert
        assertThatThrownBy(() ->
                gatewayResilienceConfig.defaultCircuitBreakerCustomizer(null)
        ).isInstanceOf(NullPointerException.class);
    }

    private ResilienceProperties createProperties() {
        ResilienceProperties properties = new ResilienceProperties();
        ResilienceProperties.CircuitBreaker circuitBreaker =
                properties.getCircuitBreaker();

        circuitBreaker.setSlidingWindowSize(30);
        circuitBreaker.setMinimumNumberOfCalls(12);
        circuitBreaker.setFailureRateThreshold(37.5f);
        circuitBreaker.setWaitDurationInOpenState(
                Duration.ofSeconds(15)
        );
        circuitBreaker.setPermittedNumberOfCallsInHalfOpenState(4);
        circuitBreaker.setTimeLimiterTimeout(
                Duration.ofSeconds(9)
        );

        return properties;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<Function<
            String,
            Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration
            >> createConfigurationCaptor() {

        return (ArgumentCaptor) ArgumentCaptor.forClass(Function.class);
    }
}
