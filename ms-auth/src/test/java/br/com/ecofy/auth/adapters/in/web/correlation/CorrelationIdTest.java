package br.com.ecofy.auth.adapters.in.web.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("Testes unitários do utilitário de correlation ID")
class CorrelationIdTest {

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve impedir a instanciação direta da classe utilitária")
    void constructor_acessoPrivado_deveImpedirInstanciacaoDireta() throws Exception {
        // Arrange
        Constructor<CorrelationId> constructor = CorrelationId.class.getDeclaredConstructor();

        // Act
        constructor.setAccessible(true);

        // Assert
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        assertThatCode(constructor::newInstance).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando o valor for nulo")
    void isValid_valorNulo_deveRetornarFalso() {
        // Arrange
        String candidate = null;

        // Act
        boolean valid = CorrelationId.isValid(candidate);

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando o valor estiver vazio")
    void isValid_valorVazio_deveRetornarFalso() {
        // Arrange
        String candidate = "";

        // Act
        boolean valid = CorrelationId.isValid(candidate);

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando o valor contiver apenas espaços")
    void isValid_valorEmBranco_deveRetornarFalso() {
        // Arrange
        String candidate = "   ";

        // Act
        boolean valid = CorrelationId.isValid(candidate);

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve aceitar o correlation ID quando o valor possuir caracteres seguros")
    void isValid_caracteresSeguros_deveRetornarVerdadeiro() {
        // Arrange
        String candidate = "request.ABC_123-xyz";

        // Act
        boolean valid = CorrelationId.isValid(candidate);

        // Assert
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar o correlation ID válido ignorando espaços nas extremidades")
    void isValid_valorValidoComEspacosExternos_deveRetornarVerdadeiro() {
        // Arrange
        String candidate = "  request-123  ";

        // Act
        boolean valid = CorrelationId.isValid(candidate);

        // Assert
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar o correlation ID quando o tamanho atingir o limite máximo")
    void isValid_tamanhoIgualAoLimite_deveRetornarVerdadeiro() {
        // Arrange
        String candidate = "a".repeat(CorrelationId.MAX_LENGTH);

        // Act
        boolean valid = CorrelationId.isValid(candidate);

        // Assert
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando o tamanho ultrapassar o limite máximo")
    void isValid_tamanhoAcimaDoLimite_deveRetornarFalso() {
        // Arrange
        String candidate = "a".repeat(CorrelationId.MAX_LENGTH + 1);

        // Act
        boolean valid = CorrelationId.isValid(candidate);

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar o correlation ID quando houver caracteres não permitidos")
    void isValid_caracteresInseguros_deveRetornarFalso() {
        // Arrange
        String candidate = "request/123\ninvalid";

        // Act
        boolean valid = CorrelationId.isValid(candidate);

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve gerar um correlation ID no formato UUID")
    void generate_quandoInvocado_deveRetornarUuidValido() {
        // Arrange
        String generated;

        // Act
        generated = CorrelationId.generate();

        // Assert
        assertThat(generated).isNotBlank();
        assertThatCode(() -> UUID.fromString(generated)).doesNotThrowAnyException();
        assertThat(CorrelationId.isValid(generated)).isTrue();
    }

    @Test
    @DisplayName("Deve recuperar o correlation ID armazenado no MDC")
    void current_correlationIdPresenteNoMdc_deveRetornarValorAssociado() {
        // Arrange
        String correlationId = "request-123";
        MDC.put(CorrelationId.MDC_KEY, correlationId);

        // Act
        String current = CorrelationId.current();

        // Assert
        assertThat(current).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve retornar nulo quando não houver correlation ID no MDC")
    void current_correlationIdAusenteNoMdc_deveRetornarNulo() {
        // Arrange
        MDC.remove(CorrelationId.MDC_KEY);

        // Act
        String current = CorrelationId.current();

        // Assert
        assertThat(current).isNull();
    }
}
