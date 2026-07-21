package br.com.ecofy.auth.config;

import br.com.ecofy.auth.adapters.out.jwt.JwtNimbusTokenProviderAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Centraliza as regras de autenticação, autorização, CORS e validação JWT.
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final JwtNimbusTokenProviderAdapter jwtNimbusTokenProviderAdapter;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String corsAllowedHeaders;

    // Registra o codificador utilizado na proteção das senhas.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // Configura as rotas públicas, as autorizações e a validação dos tokens.
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
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
                        )
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'"))
                .frameOptions(frame -> frame.sameOrigin())
        );

        return http.build();
    }

    // Converte as funções do token em autoridades reconhecidas pelo Spring Security.
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }

    // Configura as origens, os métodos e os headers permitidos pelo CORS.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(splitCsv(corsAllowedOrigins));
        config.setAllowedMethods(splitCsv(corsAllowedMethods));
        config.setAllowedHeaders(splitCsv(corsAllowedHeaders));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }

        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // Registra o decoder compartilhado para validar tokens com múltiplas chaves.
    @Bean
    public JwtDecoder jwtDecoder() {
        return jwtNimbusTokenProviderAdapter.jwtDecoder();
    }

    // Centraliza a composição dos validadores aplicados aos tokens JWT.
    static class JwtValidatorFactory {

        private final JwtProperties jwtProperties;

        JwtValidatorFactory(JwtProperties jwtProperties) {
            this.jwtProperties = jwtProperties;
        }

        // Combina as validações de emissor e tempo em uma única política.
        public OAuth2TokenValidator<Jwt> create() {
            var validators =
                    new ArrayList<OAuth2TokenValidator<Jwt>>();

            if (jwtProperties.getIssuer() != null) {
                validators.add(
                        new JwtIssuerValidator(
                                jwtProperties.getIssuer()
                        )
                );
            }

            validators.add(JwtValidators.createDefault());

            return new DelegatingOAuth2TokenValidator<>(
                    validators
            );
        }
    }
}
