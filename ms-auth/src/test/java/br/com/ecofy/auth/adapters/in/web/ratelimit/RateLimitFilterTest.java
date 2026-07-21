package br.com.ecofy.auth.adapters.in.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.ecofy.auth.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.auth.config.RateLimitProperties;
import br.com.ecofy.auth.core.domain.ratelimit.RateLimitDecision;
import br.com.ecofy.auth.core.domain.ratelimit.RateLimitPolicy;
import br.com.ecofy.auth.core.port.out.RateLimiterPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("Testes unitários do filtro de limitação de requisições")
class RateLimitFilterTest {

    private final RateLimiterPort rateLimiter = mock(RateLimiterPort.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private RateLimitProperties properties;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setTrustedProxies(List.of("127.0.0.1"));

        RateLimitProperties.Policy loginPolicy = new RateLimitProperties.Policy();
        loginPolicy.setLimit(5);
        loginPolicy.setWindow(Duration.ofMinutes(1));
        properties.setPolicies(Map.of("login", loginPolicy));

        filter = new RateLimitFilter(
                rateLimiter,
                properties,
                new ClientIpResolver(properties),
                objectMapper(),
                registry
        );
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve permitir a requisição quando o limite não for excedido")
    void doFilter_requisicaoAbaixoDoLimite_deveProsseguirComACadeia() throws Exception {
        // Arrange
        when(rateLimiter.tryConsume(any(), any()))
                .thenReturn(RateLimitDecision.allow());

        MockHttpServletRequest request = loginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest())
                .as("a cadeia deve prosseguir")
                .isNotNull();
        verify(rateLimiter).tryConsume(
                eq("login:ip:127.0.0.1"),
                any(RateLimitPolicy.class)
        );
    }

    @Test
    @DisplayName("Deve retornar erro padronizado e Retry-After quando o limite for excedido")
    void doFilter_limiteExcedido_deveRetornarErroHttp429() throws Exception {
        // Arrange
        MDC.put(CorrelationId.MDC_KEY, "corr-42");

        when(rateLimiter.tryConsume(
                eq("login:ip:127.0.0.1"),
                any(RateLimitPolicy.class)
        )).thenReturn(RateLimitDecision.deny(Duration.ofSeconds(30)));

        MockHttpServletRequest request = loginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString())
                .contains("\"errorCode\":\"RATE_LIMIT_EXCEEDED\"")
                .contains("\"status\":429")
                .contains("\"details\":[]")
                .contains("\"traceId\":\"corr-42\"")
                .doesNotContain("remaining")
                .doesNotContain("ecofy:auth:rl");
        assertThat(chain.getRequest())
                .as("a cadeia não deve prosseguir")
                .isNull();
        assertThat(registry.find("ecofy.auth.rate_limit").counter())
                .isNotNull();
        assertThat(registry.find("ecofy.auth.rate_limit").counter().count())
                .isEqualTo(1d);
    }

    @Test
    @DisplayName("Deve aplicar a mesma limitação ao endpoint versionado de login")
    void doFilter_loginVersionadoComLimiteExcedido_deveRetornarErroHttp429()
            throws Exception {
        // Arrange
        when(rateLimiter.tryConsume(
                eq("login:ip:127.0.0.1"),
                any(RateLimitPolicy.class)
        )).thenReturn(RateLimitDecision.deny(Duration.ofSeconds(10)));

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/auth/api/v1/auth/token"
        );
        request.setContextPath("/auth");
        request.setRequestURI("/auth/api/v1/auth/token");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("10");
        assertThat(response.getContentAsString())
                .contains("\"errorCode\":\"RATE_LIMIT_EXCEEDED\"");
        assertThat(chain.getRequest()).isNull();
        verify(rateLimiter).tryConsume(
                eq("login:ip:127.0.0.1"),
                any(RateLimitPolicy.class)
        );
    }

    @Test
    @DisplayName("Deve ignorar a limitação quando o caminho não estiver mapeado")
    void doFilter_caminhoNaoMapeado_deveProsseguirSemConsumirLimite()
            throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/auth/api/user/me"
        );
        request.setContextPath("/auth");
        request.setRequestURI("/auth/api/user/me");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(rateLimiter);
    }

    @Test
    @DisplayName("Deve ignorar a limitação quando o método HTTP não for POST")
    void doFilter_metodoHttpNaoPost_deveProsseguirSemConsumirLimite()
            throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/auth/api/auth/token"
        );
        request.setContextPath("/auth");
        request.setRequestURI("/auth/api/auth/token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(rateLimiter);
    }

    @Test
    @DisplayName("Deve ignorar a limitação quando o recurso estiver desabilitado")
    void doFilter_rateLimitDesabilitado_deveProsseguirSemConsumirLimite()
            throws Exception {
        // Arrange
        properties.setEnabled(false);

        MockHttpServletRequest request = loginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(chain.getRequest()).isNotNull();
        verify(rateLimiter, never()).tryConsume(any(), any());
    }

    @Test
    @DisplayName("Deve ignorar a limitação quando a operação não possuir política configurada")
    void doFilter_operacaoSemPolitica_deveProsseguirSemConsumirLimite()
            throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/auth/api/register"
        );
        request.setContextPath("/auth");
        request.setRequestURI("/auth/api/register");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(rateLimiter);
    }

    @Test
    @DisplayName("Deve remover apenas o contexto correspondente ao início da URI")
    void pathWithinApplication_contextosVariados_deveRetornarCaminhoEsperado() throws Exception {
        // Arrange
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var method = RateLimitFilter.class.getDeclaredMethod(
                "pathWithinApplication",
                jakarta.servlet.http.HttpServletRequest.class
        );
        method.setAccessible(true);

        // Act e Assert
        when(request.getRequestURI()).thenReturn("/auth/api/auth/token");
        when(request.getContextPath()).thenReturn("/auth");
        assertThat(method.invoke(filter, request))
                .isEqualTo("/api/auth/token");

        when(request.getRequestURI()).thenReturn("/api/auth/token");
        when(request.getContextPath()).thenReturn(null);
        assertThat(method.invoke(filter, request))
                .isEqualTo("/api/auth/token");

        when(request.getContextPath()).thenReturn("");
        assertThat(method.invoke(filter, request))
                .isEqualTo("/api/auth/token");

        when(request.getContextPath()).thenReturn("/gateway");
        assertThat(method.invoke(filter, request))
                .isEqualTo("/api/auth/token");

        when(request.getRequestURI()).thenReturn("/auth");
        when(request.getContextPath()).thenReturn("/auth");
        assertThat(method.invoke(filter, request))
                .isEqualTo("");
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/auth/api/auth/token"
        );
        request.setContextPath("/auth");
        request.setRequestURI("/auth/api/auth/token");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
