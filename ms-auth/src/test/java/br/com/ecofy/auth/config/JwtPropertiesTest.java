package br.com.ecofy.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Testes unitários das propriedades de configuração JWT")
class JwtPropertiesTest {

    @Test
    @DisplayName("Deve inicializar as propriedades com os valores padrão")
    void constructor_novaInstancia_deveRetornarValoresPadrao() {
        // Arrange
        JwtProperties properties = new JwtProperties();

        // Act
        String issuer = properties.getIssuer();
        String audience = properties.getAudience();
        String keyId = properties.getKeyId();
        String privateKeyLocation =
                properties.getPrivateKeyLocation();
        String publicKeyLocation =
                properties.getPublicKeyLocation();
        Long accessTokenTtlSeconds =
                properties.getAccessTokenTtlSeconds();
        Long refreshTokenTtlSeconds =
                properties.getRefreshTokenTtlSeconds();
        Long clockSkewSeconds =
                properties.getClockSkewSeconds();

        // Assert
        assertAll(
                () -> assertNull(issuer),
                () -> assertNull(audience),
                () -> assertNull(keyId),
                () -> assertNull(privateKeyLocation),
                () -> assertNull(publicKeyLocation),
                () -> assertEquals(
                        900L,
                        accessTokenTtlSeconds
                ),
                () -> assertEquals(
                        2_592_000L,
                        refreshTokenTtlSeconds
                ),
                () -> assertEquals(
                        60L,
                        clockSkewSeconds
                )
        );
    }

    @Test
    @DisplayName("Deve atualizar todas as propriedades configuráveis com os valores informados")
    void setters_valoresValidos_deveAtualizarPropriedadesConfiguraveis() {
        // Arrange
        JwtProperties properties = new JwtProperties();
        String issuer = "https://auth.test.ecofy.com";
        String audience = "ecofy-test-api";
        String keyId = "test-signing-key";
        String privateKeyLocation =
                "file:/run/secrets/private-key.pem";
        String publicKeyLocation =
                "file:/run/secrets/public-key.pem";

        // Act
        properties.setIssuer(issuer);
        properties.setAudience(audience);
        properties.setKeyId(keyId);
        properties.setPrivateKeyLocation(privateKeyLocation);
        properties.setPublicKeyLocation(publicKeyLocation);

        // Assert
        assertAll(
                () -> assertEquals(
                        issuer,
                        properties.getIssuer()
                ),
                () -> assertEquals(
                        audience,
                        properties.getAudience()
                ),
                () -> assertEquals(
                        keyId,
                        properties.getKeyId()
                ),
                () -> assertEquals(
                        privateKeyLocation,
                        properties.getPrivateKeyLocation()
                ),
                () -> assertEquals(
                        publicKeyLocation,
                        properties.getPublicKeyLocation()
                )
        );
    }

    @Test
    @DisplayName("Deve armazenar valores nulos nas propriedades configuráveis")
    void setters_valoresNulos_deveArmazenarValoresNulos() {
        // Arrange
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("issuer");
        properties.setAudience("audience");
        properties.setKeyId("key-id");
        properties.setPrivateKeyLocation("private-key");
        properties.setPublicKeyLocation("public-key");

        // Act
        properties.setIssuer(null);
        properties.setAudience(null);
        properties.setKeyId(null);
        properties.setPrivateKeyLocation(null);
        properties.setPublicKeyLocation(null);

        // Assert
        assertAll(
                () -> assertNull(properties.getIssuer()),
                () -> assertNull(properties.getAudience()),
                () -> assertNull(properties.getKeyId()),
                () -> assertNull(
                        properties.getPrivateKeyLocation()
                ),
                () -> assertNull(
                        properties.getPublicKeyLocation()
                )
        );
    }

    @Test
    @DisplayName("Deve armazenar valores vazios e em branco sem validação direta")
    void setters_valoresVaziosEEmBranco_deveArmazenarValoresInformados() {
        // Arrange
        JwtProperties properties = new JwtProperties();

        // Act
        properties.setIssuer("");
        properties.setAudience("   ");
        properties.setKeyId("");
        properties.setPrivateKeyLocation("   ");
        properties.setPublicKeyLocation("");

        // Assert
        assertAll(
                () -> assertEquals(
                        "",
                        properties.getIssuer()
                ),
                () -> assertEquals(
                        "   ",
                        properties.getAudience()
                ),
                () -> assertEquals(
                        "",
                        properties.getKeyId()
                ),
                () -> assertEquals(
                        "   ",
                        properties.getPrivateKeyLocation()
                ),
                () -> assertEquals(
                        "",
                        properties.getPublicKeyLocation()
                )
        );
    }
}
