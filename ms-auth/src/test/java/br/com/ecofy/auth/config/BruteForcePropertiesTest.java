package br.com.ecofy.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários das propriedades de proteção contra força bruta")
class BruteForcePropertiesTest {

    @Test
    @DisplayName("Deve inicializar as propriedades com os valores padrão")
    void constructor_novaInstancia_deveRetornarValoresPadrao() {
        // Arrange
        BruteForceProperties properties =
                new BruteForceProperties();

        // Act
        boolean enabled = properties.isEnabled();
        int threshold = properties.getThreshold();
        Duration initialBlock = properties.getInitialBlock();
        double multiplier = properties.getMultiplier();
        Duration maxBlock = properties.getMaxBlock();
        Duration observationWindow =
                properties.getObservationWindow();

        // Assert
        assertAll(
                () -> assertTrue(enabled),
                () -> assertEquals(5, threshold),
                () -> assertEquals(
                        Duration.ofMinutes(5),
                        initialBlock
                ),
                () -> assertEquals(2.0, multiplier),
                () -> assertEquals(
                        Duration.ofHours(24),
                        maxBlock
                ),
                () -> assertEquals(
                        Duration.ofMinutes(15),
                        observationWindow
                )
        );
    }

    @Test
    @DisplayName("Deve atualizar todas as propriedades com os valores informados")
    void setters_valoresValidos_deveAtualizarTodasAsPropriedades() {
        // Arrange
        BruteForceProperties properties =
                new BruteForceProperties();
        Duration initialBlock = Duration.ofMinutes(10);
        Duration maxBlock = Duration.ofHours(48);
        Duration observationWindow = Duration.ofMinutes(30);

        // Act
        properties.setEnabled(false);
        properties.setThreshold(10);
        properties.setInitialBlock(initialBlock);
        properties.setMultiplier(3.5);
        properties.setMaxBlock(maxBlock);
        properties.setObservationWindow(observationWindow);

        // Assert
        assertAll(
                () -> assertFalse(properties.isEnabled()),
                () -> assertEquals(
                        10,
                        properties.getThreshold()
                ),
                () -> assertEquals(
                        initialBlock,
                        properties.getInitialBlock()
                ),
                () -> assertEquals(
                        3.5,
                        properties.getMultiplier()
                ),
                () -> assertEquals(
                        maxBlock,
                        properties.getMaxBlock()
                ),
                () -> assertEquals(
                        observationWindow,
                        properties.getObservationWindow()
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar os valores mínimos declarados nas propriedades numéricas")
    void setters_valoresMinimos_deveArmazenarLimitesInformados() {
        // Arrange
        BruteForceProperties properties =
                new BruteForceProperties();

        // Act
        properties.setThreshold(1);
        properties.setMultiplier(1.0);

        // Assert
        assertAll(
                () -> assertEquals(
                        1,
                        properties.getThreshold()
                ),
                () -> assertEquals(
                        1.0,
                        properties.getMultiplier()
                )
        );
    }

    @Test
    @DisplayName("Deve armazenar valores nulos nas propriedades de duração sem validação direta")
    void setters_duracoesNulas_deveArmazenarValoresNulos() {
        // Arrange
        BruteForceProperties properties =
                new BruteForceProperties();

        // Act
        properties.setInitialBlock(null);
        properties.setMaxBlock(null);
        properties.setObservationWindow(null);

        // Assert
        assertAll(
                () -> assertNull(properties.getInitialBlock()),
                () -> assertNull(properties.getMaxBlock()),
                () -> assertNull(
                        properties.getObservationWindow()
                )
        );
    }

    @Test
    @DisplayName("Deve armazenar valores abaixo dos limites sem executar Bean Validation diretamente")
    void setters_valoresAbaixoDosLimites_deveArmazenarValoresInformados() {
        // Arrange
        BruteForceProperties properties =
                new BruteForceProperties();

        // Act
        properties.setThreshold(0);
        properties.setMultiplier(0.5);

        // Assert
        assertAll(
                () -> assertEquals(
                        0,
                        properties.getThreshold()
                ),
                () -> assertEquals(
                        0.5,
                        properties.getMultiplier()
                )
        );
    }

    @Test
    @DisplayName("Deve permitir reativar a proteção após ela ser desabilitada")
    void setEnabled_protecaoDesabilitada_devePermitirReativacao() {
        // Arrange
        BruteForceProperties properties =
                new BruteForceProperties();
        properties.setEnabled(false);

        // Act
        properties.setEnabled(true);

        // Assert
        assertTrue(properties.isEnabled());
    }
}
