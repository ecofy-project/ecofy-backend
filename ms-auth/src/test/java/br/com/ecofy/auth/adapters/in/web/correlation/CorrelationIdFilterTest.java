package br.com.ecofy.auth.adapters.in.web.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("Testes unitários do filtro de correlation ID")
class CorrelationIdFilterTest {

    private final SimpleMeterRegistry registry =
            new SimpleMeterRegistry();

    private final CorrelationIdFilter filter =
            new CorrelationIdFilter(registry);

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve gerar, propagar e contabilizar o correlation ID quando o header estiver ausente")
    void doFilter_correlationIdAusente_deveGerarPropagarContabilizarELimparMdc()
            throws Exception {
        // Arrange
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<String> correlationIdDuringChain =
                new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdDuringChain.set(
                        CorrelationId.current()
                );

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        String generatedCorrelationId =
                response.getHeader(CorrelationId.HEADER);

        assertThat(generatedCorrelationId).isNotBlank();
        assertThat(
                UUID.fromString(generatedCorrelationId)
        ).isNotNull();

        assertThat(
                request.getAttribute(
                        CorrelationId.REQUEST_ATTRIBUTE
                )
        ).isEqualTo(generatedCorrelationId);

        assertThat(
                correlationIdDuringChain.get()
        ).isEqualTo(generatedCorrelationId);

        assertThat(
                counter("ecofy.auth.correlation.missing")
        ).isEqualTo(1d);

        assertThat(
                counter("ecofy.auth.correlation.invalid")
        ).isZero();

        assertThat(CorrelationId.current()).isNull();
    }

    @Test
    @DisplayName("Deve preservar e normalizar o correlation ID quando o header for válido")
    void doFilter_correlationIdValido_devePreservarValorNormalizado()
            throws Exception {
        // Arrange
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationId.HEADER,
                "  req-abc_123.9  "
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        // Act
        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        // Assert
        assertThat(
                response.getHeader(CorrelationId.HEADER)
        ).isEqualTo("req-abc_123.9");

        assertThat(
                request.getAttribute(
                        CorrelationId.REQUEST_ATTRIBUTE
                )
        ).isEqualTo("req-abc_123.9");

        assertThat(
                counter("ecofy.auth.correlation.missing")
        ).isZero();

        assertThat(
                counter("ecofy.auth.correlation.invalid")
        ).isZero();

        assertThat(CorrelationId.current()).isNull();
    }

    @Test
    @DisplayName("Deve substituir e contabilizar o correlation ID quando o header contiver caracteres inválidos")
    void doFilter_correlationIdComCaracteresInvalidos_deveSubstituirEContabilizar()
            throws Exception {
        // Arrange
        String invalidCorrelationId = "correlation id inválido";

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationId.HEADER,
                invalidCorrelationId
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        // Act
        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        // Assert
        String generatedCorrelationId =
                response.getHeader(CorrelationId.HEADER);

        assertThat(generatedCorrelationId)
                .isNotEqualTo(invalidCorrelationId);

        assertThat(
                UUID.fromString(generatedCorrelationId)
        ).isNotNull();

        assertThat(
                request.getAttribute(
                        CorrelationId.REQUEST_ATTRIBUTE
                )
        ).isEqualTo(generatedCorrelationId);

        assertThat(
                counter("ecofy.auth.correlation.invalid")
        ).isEqualTo(1d);

        assertThat(
                counter("ecofy.auth.correlation.missing")
        ).isZero();

        assertThat(CorrelationId.current()).isNull();
    }

    @Test
    @DisplayName("Deve substituir e contabilizar o correlation ID quando o header exceder o tamanho máximo")
    void doFilter_correlationIdAcimaDoLimite_deveSubstituirEContabilizar()
            throws Exception {
        // Arrange
        String oversizedCorrelationId =
                "a".repeat(CorrelationId.MAX_LENGTH + 1);

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationId.HEADER,
                oversizedCorrelationId
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        // Act
        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        // Assert
        String generatedCorrelationId =
                response.getHeader(CorrelationId.HEADER);

        assertThat(generatedCorrelationId)
                .isNotEqualTo(oversizedCorrelationId);

        assertThat(
                UUID.fromString(generatedCorrelationId)
        ).isNotNull();

        assertThat(
                counter("ecofy.auth.correlation.invalid")
        ).isEqualTo(1d);

        assertThat(CorrelationId.current()).isNull();
    }

    @Test
    @DisplayName("Deve propagar a exceção e limpar o MDC quando a cadeia do filtro falhar")
    void doFilter_cadeiaLancaExcecao_devePropagarExcecaoELimparMdc() {
        // Arrange
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain failingChain =
                (servletRequest, servletResponse) -> {
                    assertThat(CorrelationId.current())
                            .isNotBlank();

                    throw new ServletException("boom");
                };

        // Act e Assert
        assertThatThrownBy(
                () -> filter.doFilter(
                        request,
                        response,
                        failingChain
                )
        )
                .isInstanceOf(ServletException.class)
                .hasMessage("boom");

        assertThat(CorrelationId.current()).isNull();
    }

    @Test
    @DisplayName("Deve validar valores nulos, vazios, inseguros e nos limites permitidos")
    void isValid_valoresNulosVaziosInvalidosELimites_deveRetornarResultadoEsperado() {
        // Arrange
        String maximumLengthValue =
                "a".repeat(CorrelationId.MAX_LENGTH);

        String oversizedValue =
                "a".repeat(CorrelationId.MAX_LENGTH + 1);

        String generatedCorrelationId =
                CorrelationId.generate();

        // Act
        boolean nullResult =
                CorrelationId.isValid(null);

        boolean emptyResult =
                CorrelationId.isValid("");

        boolean blankResult =
                CorrelationId.isValid("   ");

        boolean unsafeResult =
                CorrelationId.isValid("abc def");

        boolean maximumLengthResult =
                CorrelationId.isValid(maximumLengthValue);

        boolean oversizedResult =
                CorrelationId.isValid(oversizedValue);

        boolean generatedResult =
                CorrelationId.isValid(generatedCorrelationId);

        // Assert
        assertThat(nullResult).isFalse();
        assertThat(emptyResult).isFalse();
        assertThat(blankResult).isFalse();
        assertThat(unsafeResult).isFalse();
        assertThat(maximumLengthResult).isTrue();
        assertThat(oversizedResult).isFalse();
        assertThat(generatedResult).isTrue();

        assertThat(
                UUID.fromString(generatedCorrelationId)
        ).isNotNull();
    }

    private double counter(String name) {
        var counter = registry.find(name).counter();

        return counter == null
                ? 0d
                : counter.count();
    }
}
