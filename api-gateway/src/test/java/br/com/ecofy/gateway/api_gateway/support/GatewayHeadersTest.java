package br.com.ecofy.gateway.api_gateway.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes unitários dos headers e das chaves de contexto do Gateway")
class GatewayHeadersTest {

    @Test
    @DisplayName("Deve retornar o nome configurado para o header de correlation ID")
    void correlationId_constanteAcessada_deveRetornarNomeDoHeader() {
        // Act
        String result = GatewayHeaders.CORRELATION_ID;

        // Assert
        assertThat(result).isEqualTo("X-Correlation-Id");
    }

    @Test
    @DisplayName("Deve retornar a chave configurada para o atributo do exchange")
    void correlationIdAttr_constanteAcessada_deveRetornarChaveDoAtributo() {
        // Act
        String result = GatewayHeaders.CORRELATION_ID_ATTR;

        // Assert
        assertThat(result).isEqualTo("ecofy.gateway.correlationId");
    }

    @Test
    @DisplayName("Deve retornar a chave configurada para o contexto reativo")
    void correlationIdContextKey_constanteAcessada_deveRetornarChaveDoContexto() {
        // Act
        String result = GatewayHeaders.CORRELATION_ID_CONTEXT_KEY;

        // Assert
        assertThat(result).isEqualTo("correlationId");
    }

    @Test
    @DisplayName("Deve manter a classe final para impedir herança")
    void classDeclaration_classeInspecionada_deveSerFinal() {
        // Act
        int modifiers = GatewayHeaders.class.getModifiers();

        // Assert
        assertThat(Modifier.isFinal(modifiers)).isTrue();
    }

    @Test
    @DisplayName("Deve manter o construtor privado para impedir instanciação direta")
    void constructor_construtorInspecionado_deveSerPrivado() throws Exception {
        // Arrange
        Constructor<GatewayHeaders> constructor =
                GatewayHeaders.class.getDeclaredConstructor();

        // Act
        constructor.setAccessible(true);
        GatewayHeaders instance = constructor.newInstance();

        // Assert
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        assertThat(instance).isNotNull();
    }
}
