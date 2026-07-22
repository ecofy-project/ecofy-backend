package br.com.ecofy.auth.config;

import br.com.ecofy.auth.adapters.out.jwt.JwtNimbusTokenProviderAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("Testes da configuração da cadeia de filtros de segurança")
@ExtendWith(MockitoExtension.class)
class SecurityConfigSecurityFilterChainTest {

    @Test
    @DisplayName("Deve configurar todos os recursos de segurança e construir a cadeia de filtros")
    void securityFilterChain_shouldConfigureAllDsl_andBuildChain() throws Exception {
        JwtProperties props = new JwtProperties();
        props.setIssuer("https://auth.ecofy.com");

        SecurityConfig config = new SecurityConfig(props, mock(JwtNimbusTokenProviderAdapter.class));

        JwtDecoder jwtDecoder = mock(JwtDecoder.class);
        HttpSecurity http = mock(HttpSecurity.class);

        DefaultSecurityFilterChain builtChain =
                new DefaultSecurityFilterChain(org.springframework.security.web.util.matcher.AnyRequestMatcher.INSTANCE, List.of());

        when(http.build()).thenReturn(builtChain);

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Customizer<CorsConfigurer<HttpSecurity>> customizer = (Customizer<CorsConfigurer<HttpSecurity>>) inv.getArgument(0);

            @SuppressWarnings("unchecked")
            CorsConfigurer<HttpSecurity> cors = mock(CorsConfigurer.class);

            customizer.customize(cors);
            return http;
        }).when(http).cors(any());

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Customizer<CsrfConfigurer<HttpSecurity>> customizer = (Customizer<CsrfConfigurer<HttpSecurity>>) inv.getArgument(0);

            @SuppressWarnings("unchecked")
            CsrfConfigurer<HttpSecurity> csrf = mock(CsrfConfigurer.class);

            when(csrf.disable()).thenReturn(http);

            customizer.customize(csrf);
            return http;
        }).when(http).csrf(any());

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> customizer =
                    (Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>) inv.getArgument(0);

            @SuppressWarnings("unchecked")
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry =
                    mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class);

            @SuppressWarnings("unchecked")
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl =
                    mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);

            when(registry.requestMatchers(any(String[].class))).thenReturn(authorizedUrl);
            when(authorizedUrl.permitAll()).thenReturn(registry);
            when(authorizedUrl.hasRole(anyString())).thenReturn(registry);
            when(registry.anyRequest()).thenReturn(authorizedUrl);
            when(authorizedUrl.authenticated()).thenReturn(registry);

            customizer.customize(registry);
            return http;
        }).when(http).authorizeHttpRequests(any());

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> customizer =
                    (Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>>) inv.getArgument(0);

            @SuppressWarnings("unchecked")
            OAuth2ResourceServerConfigurer<HttpSecurity> oauth2 = mock(OAuth2ResourceServerConfigurer.class);

            doAnswer(jwtInv -> {
                @SuppressWarnings("unchecked")
                Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>.JwtConfigurer> jwtCustomizer =
                        (Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>.JwtConfigurer>) jwtInv.getArgument(0);

                @SuppressWarnings("unchecked")
                OAuth2ResourceServerConfigurer<HttpSecurity>.JwtConfigurer jwtCfg =
                        mock(OAuth2ResourceServerConfigurer.JwtConfigurer.class);

                when(jwtCfg.decoder(any(JwtDecoder.class))).thenReturn(jwtCfg);
                when(jwtCfg.jwtAuthenticationConverter(any())).thenReturn(jwtCfg);

                jwtCustomizer.customize(jwtCfg);
                return oauth2;
            }).when(oauth2).jwt(any());

            customizer.customize(oauth2);
            return http;
        }).when(http).oauth2ResourceServer(any());

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Customizer<HeadersConfigurer<HttpSecurity>> customizer =
                    (Customizer<HeadersConfigurer<HttpSecurity>>) inv.getArgument(0);

            @SuppressWarnings("unchecked")
            HeadersConfigurer<HttpSecurity> headers = mock(HeadersConfigurer.class);

            doAnswer(cspInv -> {
                @SuppressWarnings("unchecked")
                Customizer<HeadersConfigurer<HttpSecurity>.ContentSecurityPolicyConfig> cspCustomizer =
                        (Customizer<HeadersConfigurer<HttpSecurity>.ContentSecurityPolicyConfig>) cspInv.getArgument(0);

                @SuppressWarnings("unchecked")
                HeadersConfigurer<HttpSecurity>.ContentSecurityPolicyConfig csp =
                        mock(HeadersConfigurer.ContentSecurityPolicyConfig.class);

                when(csp.policyDirectives(anyString())).thenReturn(csp);

                cspCustomizer.customize(csp);
                return headers;
            }).when(headers).contentSecurityPolicy(any());

            doAnswer(frameInv -> {
                @SuppressWarnings("unchecked")
                Customizer<HeadersConfigurer<HttpSecurity>.FrameOptionsConfig> frameCustomizer =
                        (Customizer<HeadersConfigurer<HttpSecurity>.FrameOptionsConfig>) frameInv.getArgument(0);

                @SuppressWarnings("unchecked")
                HeadersConfigurer<HttpSecurity>.FrameOptionsConfig frame =
                        mock(HeadersConfigurer.FrameOptionsConfig.class);

                when(frame.sameOrigin()).thenReturn(headers);

                frameCustomizer.customize(frame);
                return headers;
            }).when(headers).frameOptions(any());

            customizer.customize(headers);
            return http;
        }).when(http).headers(any());

        SecurityFilterChain chain = config.securityFilterChain(http, jwtDecoder);

        assertSame(builtChain, chain);

        verify(http).cors(any());
        verify(http).csrf(any());
        verify(http).authorizeHttpRequests(any());
        verify(http).oauth2ResourceServer(any());
        verify(http).headers(any());
        verify(http).build();
        verifyNoMoreInteractions(http);
    }
}
