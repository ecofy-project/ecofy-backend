package br.com.ecofy.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários das propriedades de integração com o ms-users")
class UsersMsPropertiesTest {

    @Test
    @DisplayName("Deve armazenar e retornar todas as propriedades informadas")
    void accessors_valoresInformados_deveRetornarTodasAsPropriedades() {
        // Arrange
        boolean enabled = true;
        String baseUrl = "https://users.ecofy.com";
        String internalToken = "internal-token";

        // Act
        UsersMsProperties properties = new UsersMsProperties(
                enabled,
                baseUrl,
                internalToken
        );

        // Assert
        assertAll(
                () -> assertTrue(properties.enabled()),
                () -> assertEquals(baseUrl, properties.baseUrl()),
                () -> assertEquals(
                        internalToken,
                        properties.internalToken()
                )
        );
    }

    @Test
    @DisplayName("Deve aceitar propriedades textuais nulas e integração desabilitada")
    void accessors_valoresNulosEIntegracaoDesabilitada_deveRetornarValoresInformados() {
        // Arrange
        boolean enabled = false;

        // Act
        UsersMsProperties properties = new UsersMsProperties(
                enabled,
                null,
                null
        );

        // Assert
        assertAll(
                () -> assertFalse(properties.enabled()),
                () -> assertNull(properties.baseUrl()),
                () -> assertNull(properties.internalToken())
        );
    }

    @Test
    @DisplayName("Deve considerar iguais as propriedades com os mesmos valores")
    void equals_propriedadesComMesmosValores_deveRetornarVerdadeiro() {
        // Arrange
        UsersMsProperties firstProperties = new UsersMsProperties(
                true,
                "https://users.ecofy.com",
                "internal-token"
        );

        UsersMsProperties secondProperties = new UsersMsProperties(
                true,
                "https://users.ecofy.com",
                "internal-token"
        );

        // Act
        boolean result = firstProperties.equals(secondProperties);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar diferentes as propriedades com valores distintos, nulos ou tipos incompatíveis")
    void equals_propriedadesDiferentes_deveRetornarFalso() {
        // Arrange
        UsersMsProperties properties = new UsersMsProperties(
                true,
                "https://users.ecofy.com",
                "internal-token"
        );

        UsersMsProperties differentProperties = new UsersMsProperties(
                false,
                "http://localhost:8081",
                null
        );

        // Act
        boolean differentValuesResult =
                properties.equals(differentProperties);

        boolean nullResult = properties.equals(null);
        boolean incompatibleTypeResult = properties.equals("properties");

        // Assert
        assertAll(
                () -> assertFalse(differentValuesResult),
                () -> assertFalse(nullResult),
                () -> assertFalse(incompatibleTypeResult),
                () -> assertEquals(properties, properties)
        );
    }

    @Test
    @DisplayName("Deve gerar o mesmo código hash para propriedades iguais")
    void hashCode_propriedadesIguais_deveRetornarMesmoCodigo() {
        // Arrange
        UsersMsProperties firstProperties = new UsersMsProperties(
                true,
                "https://users.ecofy.com",
                "internal-token"
        );

        UsersMsProperties secondProperties = new UsersMsProperties(
                true,
                "https://users.ecofy.com",
                "internal-token"
        );

        UsersMsProperties differentProperties = new UsersMsProperties(
                false,
                null,
                null
        );

        // Act
        int firstHashCode = firstProperties.hashCode();
        int secondHashCode = secondProperties.hashCode();
        int differentHashCode = differentProperties.hashCode();

        // Assert
        assertAll(
                () -> assertEquals(firstHashCode, secondHashCode),
                () -> assertNotEquals(firstHashCode, differentHashCode)
        );
    }

    @Test
    @DisplayName("Deve representar todas as propriedades na conversão para texto")
    void toString_propriedadesPreenchidas_deveRetornarRepresentacaoCompleta() {
        // Arrange
        UsersMsProperties properties = new UsersMsProperties(
                true,
                "https://users.ecofy.com",
                "internal-token"
        );

        String expected =
                "UsersMsProperties[enabled=true, "
                        + "baseUrl=https://users.ecofy.com, "
                        + "internalToken=internal-token]";

        // Act
        String result = properties.toString();

        // Assert
        assertEquals(expected, result);
    }
}
