package br.com.ecofy.gateway.api_gateway;

import br.com.ecofy.gateway.api_gateway.correlation.CorrelationIdValidator;
import br.com.ecofy.gateway.api_gateway.correlation.CorrelationProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários da validação/geração de correlation ID (ECO-05 §12.1).
 * Cobre valor vazio, longo demais, caracteres inválidos e geração de UUID,
 * sem subir contexto Spring.
 */
class CorrelationIdValidatorTest {

    private final CorrelationIdValidator validator = new CorrelationIdValidator(properties(128));

    private static CorrelationProperties properties(int maxLength) {
        CorrelationProperties p = new CorrelationProperties();
        p.setMaxLength(maxLength);
        return p;
    }

    @Test
    void acceptsValidValue() {
        assertThat(validator.isValid("0af7651916cd43dd8448eb211c80319c")).isTrue();
        assertThat(validator.isValid("req-123_ABC.9")).isTrue();
    }

    @Test
    void rejectsNullEmptyOrBlank() {
        assertThat(validator.isValid(null)).isFalse();
        assertThat(validator.isValid("")).isFalse();
        assertThat(validator.isValid("   ")).isFalse();
    }

    @Test
    void rejectsTooLongValue() {
        String tooLong = "a".repeat(129);
        assertThat(validator.isValid(tooLong)).isFalse();
    }

    @Test
    void rejectsUnsafeCharacters() {
        assertThat(validator.isValid("has spaces")).isFalse();
        assertThat(validator.isValid("line\nbreak")).isFalse();
        assertThat(validator.isValid("semi;colon")).isFalse();
        assertThat(validator.isValid("<script>")).isFalse();
    }

    @Test
    void generatesUuid() {
        String generated = validator.generate();
        assertThat(generated).isNotBlank();
        // Não lança: é um UUID válido.
        assertThat(UUID.fromString(generated)).isNotNull();
        assertThat(validator.isValid(generated)).isTrue();
    }
}
