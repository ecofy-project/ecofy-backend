package br.com.ecofy.gateway.api_gateway.cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes unitários das propriedades CORS")
class CorsPropertiesTest {

    @Test
    @DisplayName("Deve retornar os valores padrão em uma nova instância")
    void getters_novaInstancia_deveRetornarValoresPadrao() {
        // Arrange
        CorsProperties properties = new CorsProperties();

        // Act
        List<String> allowedOrigins = properties.getAllowedOrigins();
        List<String> allowedMethods = properties.getAllowedMethods();
        List<String> allowedHeaders = properties.getAllowedHeaders();
        List<String> exposedHeaders = properties.getExposedHeaders();
        boolean allowCredentials = properties.isAllowCredentials();
        long maxAge = properties.getMaxAge();

        // Assert
        assertThat(allowedOrigins).isEmpty();

        assertThat(allowedMethods).containsExactly(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        );

        assertThat(allowedHeaders).containsExactly(
                "Authorization",
                "Content-Type",
                "X-Correlation-Id"
        );

        assertThat(exposedHeaders)
                .containsExactly("X-Correlation-Id");

        assertThat(allowCredentials).isFalse();
        assertThat(maxAge).isEqualTo(3600);
    }

    @Test
    @DisplayName("Deve atualizar todas as propriedades quando novos valores forem informados")
    void setters_valoresCustomizados_deveAtualizarTodasAsPropriedades() {
        // Arrange
        CorsProperties properties = new CorsProperties();

        List<String> allowedOrigins = List.of(
                "https://app.ecofy.test",
                "https://admin.ecofy.test"
        );
        List<String> allowedMethods = List.of("GET", "POST");
        List<String> allowedHeaders = List.of(
                "Authorization",
                "Content-Type"
        );
        List<String> exposedHeaders = List.of(
                "X-Correlation-Id",
                "Location"
        );

        // Act
        properties.setAllowedOrigins(allowedOrigins);
        properties.setAllowedMethods(allowedMethods);
        properties.setAllowedHeaders(allowedHeaders);
        properties.setExposedHeaders(exposedHeaders);
        properties.setAllowCredentials(true);
        properties.setMaxAge(7200);

        // Assert
        assertThat(properties.getAllowedOrigins())
                .isSameAs(allowedOrigins);
        assertThat(properties.getAllowedMethods())
                .isSameAs(allowedMethods);
        assertThat(properties.getAllowedHeaders())
                .isSameAs(allowedHeaders);
        assertThat(properties.getExposedHeaders())
                .isSameAs(exposedHeaders);
        assertThat(properties.isAllowCredentials()).isTrue();
        assertThat(properties.getMaxAge()).isEqualTo(7200);
    }

    @Test
    @DisplayName("Deve aceitar listas nulas porque a classe não possui validação interna")
    void setters_listasNulas_deveManterValoresNulos() {
        // Arrange
        CorsProperties properties = new CorsProperties();

        // Act
        properties.setAllowedOrigins(null);
        properties.setAllowedMethods(null);
        properties.setAllowedHeaders(null);
        properties.setExposedHeaders(null);

        // Assert
        assertThat(properties.getAllowedOrigins()).isNull();
        assertThat(properties.getAllowedMethods()).isNull();
        assertThat(properties.getAllowedHeaders()).isNull();
        assertThat(properties.getExposedHeaders()).isNull();
    }

    @Test
    @DisplayName("Deve aceitar zero como tempo máximo porque não existe validação interna")
    void setMaxAge_valorZero_deveAtualizarPropriedade() {
        // Arrange
        CorsProperties properties = new CorsProperties();

        // Act
        properties.setMaxAge(0);

        // Assert
        assertThat(properties.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("Deve desabilitar credenciais após a propriedade ter sido habilitada")
    void setAllowCredentials_valorFalso_deveDesabilitarCredenciais() {
        // Arrange
        CorsProperties properties = new CorsProperties();
        properties.setAllowCredentials(true);

        // Act
        properties.setAllowCredentials(false);

        // Assert
        assertThat(properties.isAllowCredentials()).isFalse();
    }
}
