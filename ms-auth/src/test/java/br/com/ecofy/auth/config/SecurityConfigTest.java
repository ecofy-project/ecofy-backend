package br.com.ecofy.auth.config;

import br.com.ecofy.auth.adapters.out.jwt.JwtNimbusTokenProviderAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Testes unitários da configuração de segurança")
class SecurityConfigTest {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/v1/auth/token",
            "/api/v1/auth/refresh",
            "/api/v1/auth/validate",
            "/api/v1/auth/revoke",
            "/api/v1/auth/register/**",
            "/api/v1/auth/password/**",
            "/api/auth/token",
            "/api/auth/refresh",
            "/api/auth/validate",
            "/api/auth/revoke",
            "/api/register/**",
            "/api/password/**",
            "/.well-known/**"
    };

    @Test
    @DisplayName("Deve criar um codificador BCrypt com força doze")
    void passwordEncoder_configuracaoPadrao_deveCriarBCryptComForcaDoze() {
        // Arrange
        SecurityConfig config = new SecurityConfig(
                mock(JwtProperties.class),
                mock(JwtNimbusTokenProviderAdapter.class)
        );

        String rawPassword = "SenhaSegura@123";

        // Act
        PasswordEncoder result = config.passwordEncoder();
        String encodedPassword = result.encode(rawPassword);

        // Assert
        assertAll(
                () -> assertInstanceOf(
                        BCryptPasswordEncoder.class,
                        result
                ),
                () -> assertTrue(
                        result.matches(
                                rawPassword,
                                encodedPassword
                        )
                ),
                () -> assertEquals(
                        "12",
                        encodedPassword.split("\\$")[2]
                )
        );
    }

    @Test
    @DisplayName("Deve configurar todas as regras de segurança e construir a cadeia de filtros")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void securityFilterChain_configuracaoCompleta_deveConstruirCadeiaDeFiltros()
            throws Exception {
        // Arrange
        JwtProperties jwtProperties =
                mock(JwtProperties.class);

        JwtNimbusTokenProviderAdapter tokenProvider =
                mock(JwtNimbusTokenProviderAdapter.class);

        SecurityConfig config = new SecurityConfig(
                jwtProperties,
                tokenProvider
        );

        HttpSecurity http = mock(HttpSecurity.class);
        JwtDecoder jwtDecoder = mock(JwtDecoder.class);

        DefaultSecurityFilterChain expectedChain =
                mock(DefaultSecurityFilterChain.class);

        CorsConfigurer<HttpSecurity> corsConfigurer =
                mock(CorsConfigurer.class);

        CsrfConfigurer<HttpSecurity> csrfConfigurer =
                mock(CsrfConfigurer.class);

        AuthorizeHttpRequestsConfigurer<HttpSecurity>
                .AuthorizationManagerRequestMatcherRegistry
                authorizationRegistry =
                mock(
                        AuthorizeHttpRequestsConfigurer
                                .AuthorizationManagerRequestMatcherRegistry
                                .class
                );

        AuthorizeHttpRequestsConfigurer<HttpSecurity>
                .AuthorizedUrl authorizedUrl =
                mock(
                        AuthorizeHttpRequestsConfigurer
                                .AuthorizedUrl
                                .class
                );

        OAuth2ResourceServerConfigurer<HttpSecurity>
                oauth2Configurer =
                mock(OAuth2ResourceServerConfigurer.class);

        OAuth2ResourceServerConfigurer<HttpSecurity>
                .JwtConfigurer jwtConfigurer =
                mock(
                        OAuth2ResourceServerConfigurer
                                .JwtConfigurer
                                .class
                );

        HeadersConfigurer<HttpSecurity> headersConfigurer =
                mock(HeadersConfigurer.class);

        HeadersConfigurer<HttpSecurity>
                .ContentSecurityPolicyConfig
                contentSecurityPolicyConfig =
                mock(
                        HeadersConfigurer
                                .ContentSecurityPolicyConfig
                                .class
                );

        HeadersConfigurer<HttpSecurity>
                .FrameOptionsConfig frameOptionsConfig =
                mock(
                        HeadersConfigurer
                                .FrameOptionsConfig
                                .class
                );

        doAnswer(invocation -> {
            Customizer<CorsConfigurer<HttpSecurity>> customizer =
                    invocation.getArgument(0);

            customizer.customize(corsConfigurer);
            return http;
        }).when(http).cors(any(Customizer.class));

        when(csrfConfigurer.disable())
                .thenReturn(http);

        doAnswer(invocation -> {
            Customizer<CsrfConfigurer<HttpSecurity>> customizer =
                    invocation.getArgument(0);

            customizer.customize(csrfConfigurer);
            return http;
        }).when(http).csrf(any(Customizer.class));

        when(
                authorizationRegistry.requestMatchers(
                        any(String[].class)
                )
        ).thenReturn(authorizedUrl);

        when(authorizedUrl.permitAll())
                .thenReturn(authorizationRegistry);

        when(authorizedUrl.hasRole("ADMIN"))
                .thenReturn(authorizationRegistry);

        when(authorizationRegistry.anyRequest())
                .thenReturn(authorizedUrl);

        when(authorizedUrl.authenticated())
                .thenReturn(authorizationRegistry);

        doAnswer(invocation -> {
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry>
                    customizer = invocation.getArgument(0);

            customizer.customize(authorizationRegistry);
            return http;
        }).when(http).authorizeHttpRequests(
                any(Customizer.class)
        );

        when(jwtConfigurer.decoder(jwtDecoder))
                .thenReturn(jwtConfigurer);

        when(
                jwtConfigurer.jwtAuthenticationConverter(
                        any(JwtAuthenticationConverter.class)
                )
        ).thenReturn(jwtConfigurer);

        doAnswer(invocation -> {
            Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>
                    .JwtConfigurer> customizer =
                    invocation.getArgument(0);

            customizer.customize(jwtConfigurer);
            return oauth2Configurer;
        }).when(oauth2Configurer).jwt(
                any(Customizer.class)
        );

        doAnswer(invocation -> {
            Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>>
                    customizer = invocation.getArgument(0);

            customizer.customize(oauth2Configurer);
            return http;
        }).when(http).oauth2ResourceServer(
                any(Customizer.class)
        );

        when(
                contentSecurityPolicyConfig.policyDirectives(
                        "default-src 'self'"
                )
        ).thenReturn(contentSecurityPolicyConfig);

        doAnswer(invocation -> {
            Customizer<HeadersConfigurer<HttpSecurity>
                    .ContentSecurityPolicyConfig>
                    customizer = invocation.getArgument(0);

            customizer.customize(contentSecurityPolicyConfig);
            return headersConfigurer;
        }).when(headersConfigurer).contentSecurityPolicy(
                any(Customizer.class)
        );

        when(frameOptionsConfig.sameOrigin())
                .thenReturn(headersConfigurer);

        doAnswer(invocation -> {
            Customizer<HeadersConfigurer<HttpSecurity>
                    .FrameOptionsConfig> customizer =
                    invocation.getArgument(0);

            customizer.customize(frameOptionsConfig);
            return headersConfigurer;
        }).when(headersConfigurer).frameOptions(
                any(Customizer.class)
        );

        doAnswer(invocation -> {
            Customizer<HeadersConfigurer<HttpSecurity>> customizer =
                    invocation.getArgument(0);

            customizer.customize(headersConfigurer);
            return http;
        }).when(http).headers(any(Customizer.class));

        when(http.build())
                .thenReturn(expectedChain);

        // Act
        SecurityFilterChain result =
                config.securityFilterChain(
                        http,
                        jwtDecoder
                );

        // Assert
        assertSame(expectedChain, result);

        verify(http).cors(any(Customizer.class));
        verify(csrfConfigurer).disable();

        verify(authorizationRegistry)
                .requestMatchers(PUBLIC_ENDPOINTS);

        verify(authorizedUrl).permitAll();

        verify(authorizationRegistry)
                .requestMatchers("/api/admin/**");

        verify(authorizedUrl).hasRole("ADMIN");
        verify(authorizationRegistry).anyRequest();
        verify(authorizedUrl).authenticated();

        verify(jwtConfigurer).decoder(jwtDecoder);

        verify(jwtConfigurer)
                .jwtAuthenticationConverter(
                        any(JwtAuthenticationConverter.class)
                );

        verify(contentSecurityPolicyConfig)
                .policyDirectives("default-src 'self'");

        verify(frameOptionsConfig).sameOrigin();
        verify(http).build();
    }

    @Test
    @DisplayName("Deve converter as funções do token em autoridades sem adicionar prefixo")
    void jwtAuthenticationConverter_tokenComFuncoes_deveConverterAutoridadesSemPrefixo() {
        // Arrange
        SecurityConfig config = new SecurityConfig(
                mock(JwtProperties.class),
                mock(JwtNimbusTokenProviderAdapter.class)
        );

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-id")
                .claim(
                        "roles",
                        List.of(
                                "ROLE_ADMIN",
                                "USER_READ"
                        )
                )
                .build();

        // Act
        JwtAuthenticationConverter converter =
                config.jwtAuthenticationConverter();

        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) converter.convert(jwt);

        // Assert
        assertNotNull(authentication);

        List<String> authorities =
                authentication.getAuthorities()
                        .stream()
                        .map(authority -> authority.getAuthority())
                        .toList();

        assertAll(
                () -> assertTrue(
                        authorities.contains("ROLE_ADMIN")
                ),
                () -> assertTrue(
                        authorities.contains("USER_READ")
                ),
                () -> assertFalse(
                        authorities.contains("SCOPE_ROLE_ADMIN")
                ),
                () -> assertFalse(
                        authorities.contains("SCOPE_USER_READ")
                )
        );
    }

    @Test
    @DisplayName("Deve configurar origens, métodos e cabeçalhos após remover espaços e valores vazios")
    void corsConfigurationSource_valoresCsv_deveRegistrarConfiguracaoNormalizada() {
        // Arrange
        SecurityConfig config = new SecurityConfig(
                mock(JwtProperties.class),
                mock(JwtNimbusTokenProviderAdapter.class)
        );

        ReflectionTestUtils.setField(
                config,
                "corsAllowedOrigins",
                " https://app.ecofy.com, ,http://localhost:3000 "
        );

        ReflectionTestUtils.setField(
                config,
                "corsAllowedMethods",
                " GET,POST, ,OPTIONS "
        );

        ReflectionTestUtils.setField(
                config,
                "corsAllowedHeaders",
                " Authorization, Content-Type, "
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/v1/auth/token"
                );

        // Act
        CorsConfigurationSource source =
                config.corsConfigurationSource();

        CorsConfiguration result =
                source.getCorsConfiguration(request);

        // Assert
        assertNotNull(result);

        assertAll(
                () -> assertEquals(
                        List.of(
                                "https://app.ecofy.com",
                                "http://localhost:3000"
                        ),
                        result.getAllowedOrigins()
                ),
                () -> assertEquals(
                        List.of(
                                "GET",
                                "POST",
                                "OPTIONS"
                        ),
                        result.getAllowedMethods()
                ),
                () -> assertEquals(
                        List.of(
                                "Authorization",
                                "Content-Type"
                        ),
                        result.getAllowedHeaders()
                ),
                () -> assertEquals(
                        Boolean.TRUE,
                        result.getAllowCredentials()
                ),
                () -> assertEquals(
                        3600L,
                        result.getMaxAge()
                )
        );
    }

    @Test
    @DisplayName("Deve registrar listas vazias quando os valores do CORS forem nulos, em branco ou vazios")
    void corsConfigurationSource_valoresAusentes_deveRegistrarListasVazias() {
        // Arrange
        SecurityConfig config = new SecurityConfig(
                mock(JwtProperties.class),
                mock(JwtNimbusTokenProviderAdapter.class)
        );

        ReflectionTestUtils.setField(
                config,
                "corsAllowedOrigins",
                null
        );

        ReflectionTestUtils.setField(
                config,
                "corsAllowedMethods",
                "   "
        );

        ReflectionTestUtils.setField(
                config,
                "corsAllowedHeaders",
                ", ,"
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "OPTIONS",
                        "/qualquer-rota"
                );

        // Act
        CorsConfigurationSource source =
                config.corsConfigurationSource();

        CorsConfiguration result =
                source.getCorsConfiguration(request);

        // Assert
        assertNotNull(result);

        assertAll(
                () -> assertNotNull(
                        result.getAllowedOrigins()
                ),
                () -> assertTrue(
                        result.getAllowedOrigins().isEmpty()
                ),
                () -> assertNotNull(
                        result.getAllowedMethods()
                ),
                () -> assertTrue(
                        result.getAllowedMethods().isEmpty()
                ),
                () -> assertNotNull(
                        result.getAllowedHeaders()
                ),
                () -> assertTrue(
                        result.getAllowedHeaders().isEmpty()
                )
        );
    }

    @Test
    @DisplayName("Deve retornar o decoder JWT fornecido pelo adaptador de tokens")
    void jwtDecoder_decoderDisponivel_deveRetornarInstanciaDoAdaptador() {
        // Arrange
        JwtNimbusTokenProviderAdapter tokenProvider =
                mock(JwtNimbusTokenProviderAdapter.class);

        NimbusJwtDecoder expectedDecoder =
                mock(NimbusJwtDecoder.class);

        when(tokenProvider.jwtDecoder())
                .thenReturn(expectedDecoder);

        SecurityConfig config = new SecurityConfig(
                mock(JwtProperties.class),
                tokenProvider
        );

        // Act
        JwtDecoder result = config.jwtDecoder();

        // Assert
        assertSame(expectedDecoder, result);
        verify(tokenProvider).jwtDecoder();
    }

    @Test
    @DisplayName("Deve validar o emissor e o tempo quando um emissor estiver configurado")
    void create_emissorConfigurado_deveAplicarValidacaoDeEmissorETempo() {
        // Arrange
        String expectedIssuer = "https://auth.ecofy.com";

        JwtProperties properties =
                mock(JwtProperties.class);

        when(properties.getIssuer())
                .thenReturn(expectedIssuer);

        SecurityConfig.JwtValidatorFactory factory =
                new SecurityConfig.JwtValidatorFactory(properties);

        Instant now = Instant.now();

        Jwt validJwt = Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .issuer(expectedIssuer)
                .issuedAt(now.minusSeconds(30))
                .expiresAt(now.plusSeconds(300))
                .build();

        Jwt invalidIssuerJwt = Jwt.withTokenValue("invalid-token")
                .header("alg", "RS256")
                .issuer("https://invalid-issuer.com")
                .issuedAt(now.minusSeconds(30))
                .expiresAt(now.plusSeconds(300))
                .build();

        // Act
        OAuth2TokenValidator<Jwt> validator =
                factory.create();

        OAuth2TokenValidatorResult validResult =
                validator.validate(validJwt);

        OAuth2TokenValidatorResult invalidResult =
                validator.validate(invalidIssuerJwt);

        // Assert
        assertAll(
                () -> assertFalse(validResult.hasErrors()),
                () -> assertTrue(invalidResult.hasErrors())
        );

        verify(properties, times(2)).getIssuer();
    }

    @Test
    @DisplayName("Deve aplicar somente a validação padrão quando o emissor não estiver configurado")
    void create_emissorNulo_deveAplicarSomenteValidacaoPadrao() {
        // Arrange
        JwtProperties properties =
                mock(JwtProperties.class);

        when(properties.getIssuer()).thenReturn(null);

        SecurityConfig.JwtValidatorFactory factory =
                new SecurityConfig.JwtValidatorFactory(
                        properties
                );

        Instant now = Instant.now();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(now.minusSeconds(30))
                .expiresAt(now.plusSeconds(300))
                .build();

        // Act
        OAuth2TokenValidator<Jwt> validator =
                factory.create();

        OAuth2TokenValidatorResult result =
                validator.validate(jwt);

        // Assert
        assertFalse(result.hasErrors());
        verify(properties).getIssuer();
    }
}
