package br.com.ecofy.auth.config;

import br.com.ecofy.auth.core.domain.ratelimit.RateLimitPolicy;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários das propriedades de rate limiting")
class RateLimitPropertiesTest {

    @Test
    @DisplayName("Deve inicializar as propriedades com os valores padrão")
    void constructor_novaInstancia_deveRetornarValoresPadrao() {
        // Arrange
        RateLimitProperties properties =
                new RateLimitProperties();

        // Act
        boolean enabled = properties.isEnabled();
        List<String> trustedProxies =
                properties.getTrustedProxies();
        Map<String, RateLimitProperties.Policy> policies =
                properties.getPolicies();

        // Assert
        assertAll(
                () -> assertTrue(enabled),
                () -> assertEquals(
                        List.of(
                                "127.0.0.1",
                                "::1",
                                "10.",
                                "172.",
                                "192.168."
                        ),
                        trustedProxies
                ),
                () -> assertNotNull(policies),
                () -> assertTrue(policies.isEmpty())
        );
    }

    @Test
    @DisplayName("Deve inicializar uma política sem limite e sem janela")
    void constructor_novaPolitica_deveRetornarValoresPadrao() {
        // Arrange
        RateLimitProperties.Policy policy =
                new RateLimitProperties.Policy();

        // Act
        int limit = policy.getLimit();
        Duration window = policy.getWindow();

        // Assert
        assertAll(
                () -> assertEquals(0, limit),
                () -> assertNull(window)
        );
    }

    @Test
    @DisplayName("Deve atualizar todas as propriedades com os valores informados")
    void setters_valoresValidos_deveAtualizarTodasAsPropriedades() {
        // Arrange
        RateLimitProperties properties =
                new RateLimitProperties();

        List<String> trustedProxies =
                new ArrayList<>(List.of("203.0.113."));

        RateLimitProperties.Policy policy =
                new RateLimitProperties.Policy();

        Duration window = Duration.ofMinutes(5);

        policy.setLimit(10);
        policy.setWindow(window);

        Map<String, RateLimitProperties.Policy> policies =
                new LinkedHashMap<>();
        policies.put("login", policy);

        // Act
        properties.setEnabled(false);
        properties.setTrustedProxies(trustedProxies);
        properties.setPolicies(policies);

        // Assert
        assertAll(
                () -> assertFalse(properties.isEnabled()),
                () -> assertSame(
                        trustedProxies,
                        properties.getTrustedProxies()
                ),
                () -> assertSame(
                        policies,
                        properties.getPolicies()
                ),
                () -> assertEquals(10, policy.getLimit()),
                () -> assertSame(window, policy.getWindow())
        );
    }

    @Test
    @DisplayName("Deve substituir coleções nulas por coleções vazias")
    void setters_colecoesNulas_deveArmazenarColecoesVazias() {
        // Arrange
        RateLimitProperties properties =
                new RateLimitProperties();

        List<String> originalTrustedProxies =
                properties.getTrustedProxies();
        Map<String, RateLimitProperties.Policy> originalPolicies =
                properties.getPolicies();

        // Act
        properties.setTrustedProxies(null);
        properties.setPolicies(null);

        // Assert
        assertAll(
                () -> assertNotNull(
                        properties.getTrustedProxies()
                ),
                () -> assertTrue(
                        properties.getTrustedProxies().isEmpty()
                ),
                () -> assertNotNull(properties.getPolicies()),
                () -> assertTrue(
                        properties.getPolicies().isEmpty()
                ),
                () -> assertNotSame(
                        originalTrustedProxies,
                        properties.getTrustedProxies()
                ),
                () -> assertNotSame(
                        originalPolicies,
                        properties.getPolicies()
                )
        );
    }

    @Test
    @DisplayName("Deve retornar a política de domínio quando a operação estiver configurada")
    void policyFor_operacaoConfigurada_deveRetornarPoliticaDeDominio() {
        // Arrange
        RateLimitProperties properties =
                new RateLimitProperties();

        RateLimitProperties.Policy configuredPolicy =
                new RateLimitProperties.Policy();

        Duration window = Duration.ofMinutes(1);

        configuredPolicy.setLimit(5);
        configuredPolicy.setWindow(window);

        properties.setPolicies(
                Map.of("login", configuredPolicy)
        );

        // Act
        Optional<RateLimitPolicy> optionalResult =
                properties.policyFor("login");

        RateLimitPolicy result =
                optionalResult.orElseThrow();

        // Assert
        assertAll(
                () -> assertTrue(optionalResult.isPresent()),
                () -> assertEquals(
                        "login",
                        result.name()
                ),
                () -> assertEquals(5, result.limit()),
                () -> assertEquals(window, result.window())
        );
    }

    @Test
    @DisplayName("Deve retornar vazio quando a operação não estiver configurada")
    void policyFor_operacaoNaoConfigurada_deveRetornarOptionalVazio() {
        // Arrange
        RateLimitProperties properties =
                new RateLimitProperties();

        // Act
        Optional<RateLimitPolicy> unknownOperation =
                properties.policyFor("unknown");

        Optional<RateLimitPolicy> nullOperation =
                properties.policyFor(null);

        // Assert
        assertAll(
                () -> assertTrue(unknownOperation.isEmpty()),
                () -> assertTrue(nullOperation.isEmpty())
        );
    }

    @Test
    @DisplayName("Deve identificar limite abaixo do mínimo e janela nula como configurações inválidas")
    void validation_politicaInvalida_deveRetornarViolacoes() {
        // Arrange
        RateLimitProperties properties =
                new RateLimitProperties();

        RateLimitProperties.Policy policy =
                new RateLimitProperties.Policy();

        policy.setLimit(0);
        policy.setWindow(null);

        properties.setPolicies(Map.of("login", policy));

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            // Act
            Set<ConstraintViolation<RateLimitProperties>> violations =
                    validator.validate(properties);

            // Assert
            assertAll(
                    () -> assertEquals(2, violations.size()),
                    () -> assertTrue(
                            violations.stream().anyMatch(
                                    violation ->
                                            violation
                                                    .getConstraintDescriptor()
                                                    .getAnnotation()
                                                    instanceof Min
                            )
                    ),
                    () -> assertTrue(
                            violations.stream().anyMatch(
                                    violation ->
                                            violation
                                                    .getConstraintDescriptor()
                                                    .getAnnotation()
                                                    instanceof NotNull
                            )
                    )
            );
        }
    }

    @Test
    @DisplayName("Deve aceitar o limite mínimo e uma janela não nula")
    void validation_politicaNoLimiteMinimo_deveRetornarSemViolacoes() {
        // Arrange
        RateLimitProperties properties =
                new RateLimitProperties();

        RateLimitProperties.Policy policy =
                new RateLimitProperties.Policy();

        policy.setLimit(1);
        policy.setWindow(Duration.ofSeconds(1));

        properties.setPolicies(
                Map.of("refresh-token", policy)
        );

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            // Act
            Set<ConstraintViolation<RateLimitProperties>> violations =
                    validator.validate(properties);

            // Assert
            assertTrue(violations.isEmpty());
        }
    }
}
