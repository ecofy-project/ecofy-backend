package br.com.ecofy.auth.adapters.in.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ecofy.auth.config.RateLimitProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("Testes unitários do resolvedor de endereço IP do cliente")
class ClientIpResolverTest {

    @Test
    @DisplayName("Deve usar o primeiro endereço encaminhado quando a requisição vier de proxy confiável")
    void resolve_proxyConfiavelComForwardedFor_deveRetornarPrimeiroEnderecoEncaminhado() {
        // Arrange
        ClientIpResolver resolver = resolverWithTrusted(List.of("10."));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader(
                "X-Forwarded-For",
                "203.0.113.9, 10.0.0.5"
        );

        // Act
        String resolvedIp = resolver.resolve(request);

        // Assert
        assertThat(resolvedIp).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("Deve ignorar o endereço encaminhado quando a origem não for confiável")
    void resolve_origemNaoConfiavelComForwardedFor_deveRetornarEnderecoRemoto() {
        // Arrange
        ClientIpResolver resolver = resolverWithTrusted(List.of("10."));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.77");
        request.addHeader("X-Forwarded-For", "1.1.1.1");

        // Act
        String resolvedIp = resolver.resolve(request);

        // Assert
        assertThat(resolvedIp).isEqualTo("203.0.113.77");
    }

    @Test
    @DisplayName("Deve usar o endereço remoto quando o proxy confiável não informar endereço encaminhado")
    void resolve_proxyConfiavelSemForwardedFor_deveRetornarEnderecoRemoto() {
        // Arrange
        ClientIpResolver resolver = resolverWithTrusted(List.of("10."));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");

        // Act
        String resolvedIp = resolver.resolve(request);

        // Assert
        assertThat(resolvedIp).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("Deve usar o endereço remoto quando o endereço encaminhado estiver vazio")
    void resolve_proxyConfiavelComForwardedForVazio_deveRetornarEnderecoRemoto() {
        // Arrange
        ClientIpResolver resolver = resolverWithTrusted(List.of("10."));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "   ");

        // Act
        String resolvedIp = resolver.resolve(request);

        // Assert
        assertThat(resolvedIp).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("Deve rejeitar o endereço encaminhado quando houver caracteres inseguros")
    void resolve_proxyConfiavelComForwardedForInseguro_deveRetornarEnderecoRemoto() {
        // Arrange
        ClientIpResolver resolver = resolverWithTrusted(List.of("10."));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader(
                "X-Forwarded-For",
                "not an ip; drop table"
        );

        // Act
        String resolvedIp = resolver.resolve(request);

        // Assert
        assertThat(resolvedIp).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("Deve retornar desconhecido quando o endereço remoto estiver ausente")
    void resolve_enderecoRemotoAusente_deveRetornarDesconhecido() {
        // Arrange
        ClientIpResolver resolver = resolverWithTrusted(List.of("10."));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(null);

        // Act
        String resolvedIp = resolver.resolve(request);

        // Assert
        assertThat(resolvedIp).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Deve ignorar o endereço encaminhado quando não houver proxies confiáveis")
    void resolve_semProxiesConfiaveis_deveRetornarEnderecoRemoto() {
        // Arrange
        ClientIpResolver resolver = resolverWithTrusted(List.of());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.77");
        request.addHeader("X-Forwarded-For", "1.1.1.1");

        // Act
        String resolvedIp = resolver.resolve(request);

        // Assert
        assertThat(resolvedIp).isEqualTo("203.0.113.77");
    }

    private ClientIpResolver resolverWithTrusted(List<String> trustedProxies) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustedProxies(trustedProxies);
        return new ClientIpResolver(properties);
    }
}
