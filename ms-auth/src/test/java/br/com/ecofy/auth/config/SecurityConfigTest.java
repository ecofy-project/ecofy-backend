package br.com.ecofy.auth.config;

import br.com.ecofy.auth.adapters.out.jwt.JwtNimbusTokenProviderAdapter;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtNimbusTokenProviderAdapter jwtNimbusTokenProviderAdapter;

    @Test
    void passwordEncoder_shouldEncodeAndMatch() {
        JwtProperties props = new JwtProperties();
        SecurityConfig config = new SecurityConfig(props, jwtNimbusTokenProviderAdapter);

        PasswordEncoder encoder = config.passwordEncoder();

        String raw = "pass-123";
        String hash = encoder.encode(raw);

        assertNotNull(hash);
        assertNotEquals(raw, hash);
        assertTrue(encoder.matches(raw, hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    void securityFilterChain_shouldConfigureAllDsl_andBuildChain() throws Exception {
        JwtProperties props = new JwtProperties();
        SecurityConfig config = new SecurityConfig(props, jwtNimbusTokenProviderAdapter);

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

                when(csp.policyDirectives(any())).thenReturn(csp);

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

    @Test
    void jwtDecoder_shouldCreateNimbusJwtDecoder_andSetValidator_whenPublicKeyIsAvailable() throws Exception {
        JwtProperties props = new JwtProperties();
        props.setIssuer("https://issuer.test");

        RSAKey jwk = rsaKey();
        when(jwtNimbusTokenProviderAdapter.toRsaJwk()).thenReturn(jwk);

        SecurityConfig config = new SecurityConfig(props, jwtNimbusTokenProviderAdapter);

        JwtDecoder decoder = config.jwtDecoder();

        assertNotNull(decoder);
        assertInstanceOf(NimbusJwtDecoder.class, decoder);

        verify(jwtNimbusTokenProviderAdapter).toRsaJwk();
        verifyNoMoreInteractions(jwtNimbusTokenProviderAdapter);
    }

    @Test
    void jwtDecoder_shouldThrowIllegalStateException_whenPublicKeyExtractionThrowsJoseException() throws Exception {
        JwtProperties props = new JwtProperties();

        RSAKey rsaKey = mock(RSAKey.class);
        when(jwtNimbusTokenProviderAdapter.toRsaJwk()).thenReturn(rsaKey);
        when(rsaKey.toRSAPublicKey()).thenThrow(new JOSEException("boom"));

        SecurityConfig config = new SecurityConfig(props, jwtNimbusTokenProviderAdapter);

        IllegalStateException ex = assertThrows(IllegalStateException.class, config::jwtDecoder);
        assertEquals("Could not create JwtDecoder from in-memory JWK", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals("boom", ex.getCause().getMessage());

        verify(jwtNimbusTokenProviderAdapter).toRsaJwk();
        verify(rsaKey).toRSAPublicKey();
        verifyNoMoreInteractions(jwtNimbusTokenProviderAdapter, rsaKey);
    }

    @Test
    void jwtValidatorFactory_shouldEnforceIssuer_whenIssuerProvided_andNotEnforceWhenNull() {
        JwtProperties withIssuer = new JwtProperties();
        withIssuer.setIssuer("https://issuer.ok");

        OAuth2TokenValidator<Jwt> v1 = new SecurityConfig.JwtValidatorFactory(withIssuer).create();

        Instant now = Instant.now();

        Jwt ok = Jwt.withTokenValue("t")
                .headers(h -> h.put("alg", "none"))
                .claims(c -> {
                    c.put("sub", UUID.randomUUID().toString());
                    c.put("iss", "https://issuer.ok");
                })
                .issuedAt(now.minusSeconds(5))
                .expiresAt(now.plusSeconds(60))
                .build();

        OAuth2TokenValidatorResult okRes = v1.validate(ok);
        assertFalse(okRes.hasErrors());

        Jwt bad = Jwt.withTokenValue("t")
                .headers(h -> h.put("alg", "none"))
                .claims(c -> {
                    c.put("sub", UUID.randomUUID().toString());
                    c.put("iss", "https://issuer.bad");
                })
                .issuedAt(now.minusSeconds(5))
                .expiresAt(now.plusSeconds(60))
                .build();

        OAuth2TokenValidatorResult badRes = v1.validate(bad);
        assertTrue(badRes.hasErrors());

        JwtProperties noIssuer = new JwtProperties();
        OAuth2TokenValidator<Jwt> v2 = new SecurityConfig.JwtValidatorFactory(noIssuer).create();

        Jwt anyIssuer = Jwt.withTokenValue("t")
                .headers(h -> h.putAll(Map.of("alg", "none")))
                .claims(c -> {
                    c.put("sub", UUID.randomUUID().toString());
                    c.put("iss", "https://any-issuer");
                })
                .issuedAt(now.minusSeconds(5))
                .expiresAt(now.plusSeconds(60))
                .build();

        OAuth2TokenValidatorResult res = v2.validate(anyIssuer);
        assertFalse(res.hasErrors());
    }

    // heapers

    private static RSAKey rsaKey() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        return new RSAKey.Builder(pub).keyID("kid-1").build();
    }
}