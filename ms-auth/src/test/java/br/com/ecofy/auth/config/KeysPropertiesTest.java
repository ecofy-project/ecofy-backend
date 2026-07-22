package br.com.ecofy.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários das propriedades de chaves de autenticação")
class KeysPropertiesTest {

    @Test
    @DisplayName("Deve inicializar as propriedades com os valores padrão")
    void constructor_novaInstancia_deveRetornarValoresPadrao() {
        // Arrange
        KeysProperties properties = new KeysProperties();

        // Act
        boolean allowGeneratedKey =
                properties.isAllowGeneratedKey();
        String activeKid = properties.getActiveKid();
        String algorithm = properties.getAlgorithm();
        String activePrivateKey =
                properties.getActivePrivateKey();
        String activePrivateKeyLocation =
                properties.getActivePrivateKeyLocation();
        Duration retentionWindow =
                properties.getRetentionWindow();
        List<KeysProperties.RetiringKey> retiring =
                properties.getRetiring();

        // Assert
        assertAll(
                () -> assertTrue(allowGeneratedKey),
                () -> assertEquals(
                        "local-dev-key",
                        activeKid
                ),
                () -> assertEquals("RS256", algorithm),
                () -> assertNull(activePrivateKey),
                () -> assertNull(activePrivateKeyLocation),
                () -> assertEquals(
                        Duration.ofHours(24),
                        retentionWindow
                ),
                () -> assertNotNull(retiring),
                () -> assertTrue(retiring.isEmpty())
        );
    }

    @Test
    @DisplayName("Deve atualizar todas as propriedades com os valores informados")
    void setters_valoresValidos_deveAtualizarTodasAsPropriedades() {
        // Arrange
        KeysProperties properties = new KeysProperties();
        Duration retentionWindow = Duration.ofHours(48);

        KeysProperties.RetiringKey retiringKey =
                new KeysProperties.RetiringKey();
        retiringKey.setKid("retiring-key");
        retiringKey.setPublicKey(
                "-----BEGIN PUBLIC KEY-----"
        );
        retiringKey.setPublicKeyLocation(
                "file:/run/secrets/public-key.pem"
        );

        List<KeysProperties.RetiringKey> retiring =
                new ArrayList<>();
        retiring.add(retiringKey);

        // Act
        properties.setAllowGeneratedKey(false);
        properties.setActiveKid("production-key");
        properties.setAlgorithm("RS512");
        properties.setActivePrivateKey(
                "-----BEGIN PRIVATE KEY-----"
        );
        properties.setActivePrivateKeyLocation(
                "file:/run/secrets/private-key.pem"
        );
        properties.setRetentionWindow(retentionWindow);
        properties.setRetiring(retiring);

        // Assert
        assertAll(
                () -> assertFalse(
                        properties.isAllowGeneratedKey()
                ),
                () -> assertEquals(
                        "production-key",
                        properties.getActiveKid()
                ),
                () -> assertEquals(
                        "RS512",
                        properties.getAlgorithm()
                ),
                () -> assertEquals(
                        "-----BEGIN PRIVATE KEY-----",
                        properties.getActivePrivateKey()
                ),
                () -> assertEquals(
                        "file:/run/secrets/private-key.pem",
                        properties.getActivePrivateKeyLocation()
                ),
                () -> assertEquals(
                        retentionWindow,
                        properties.getRetentionWindow()
                ),
                () -> assertSame(
                        retiring,
                        properties.getRetiring()
                ),
                () -> assertEquals(
                        "retiring-key",
                        retiringKey.getKid()
                ),
                () -> assertEquals(
                        "-----BEGIN PUBLIC KEY-----",
                        retiringKey.getPublicKey()
                ),
                () -> assertEquals(
                        "file:/run/secrets/public-key.pem",
                        retiringKey.getPublicKeyLocation()
                )
        );
    }

    @Test
    @DisplayName("Deve substituir uma lista nula de chaves em rotação por uma lista vazia")
    void setRetiring_listaNula_deveArmazenarNovaListaVazia() {
        // Arrange
        KeysProperties properties = new KeysProperties();
        List<KeysProperties.RetiringKey> original =
                properties.getRetiring();
        original.add(new KeysProperties.RetiringKey());

        // Act
        properties.setRetiring(null);

        // Assert
        assertAll(
                () -> assertNotNull(properties.getRetiring()),
                () -> assertTrue(
                        properties.getRetiring().isEmpty()
                ),
                () -> assertFalse(
                        original == properties.getRetiring()
                )
        );
    }

    @Test
    @DisplayName("Deve armazenar valores nulos nas propriedades que não possuem validação direta")
    void setters_valoresNulos_deveArmazenarValoresNulos() {
        // Arrange
        KeysProperties properties = new KeysProperties();
        KeysProperties.RetiringKey retiringKey =
                new KeysProperties.RetiringKey();

        // Act
        properties.setActiveKid(null);
        properties.setAlgorithm(null);
        properties.setActivePrivateKey(null);
        properties.setActivePrivateKeyLocation(null);
        properties.setRetentionWindow(null);

        retiringKey.setKid(null);
        retiringKey.setPublicKey(null);
        retiringKey.setPublicKeyLocation(null);

        // Assert
        assertAll(
                () -> assertNull(properties.getActiveKid()),
                () -> assertNull(properties.getAlgorithm()),
                () -> assertNull(
                        properties.getActivePrivateKey()
                ),
                () -> assertNull(
                        properties.getActivePrivateKeyLocation()
                ),
                () -> assertNull(
                        properties.getRetentionWindow()
                ),
                () -> assertNull(retiringKey.getKid()),
                () -> assertNull(retiringKey.getPublicKey()),
                () -> assertNull(
                        retiringKey.getPublicKeyLocation()
                )
        );
    }
}
