package br.com.ecofy.auth.config;

import br.com.ecofy.auth.adapters.out.jwt.JwtNimbusTokenProviderAdapter;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
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

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final JwtNimbusTokenProviderAdapter jwtNimbusTokenProviderAdapter;

    // CORS declarado em application.yaml (cors.allowed-*) e aplicado ao Spring Security.
    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String corsAllowedHeaders;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",

                                // OpenAPI / Swagger
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",

                                // AuthController
                                "/api/auth/token",
                                "/api/auth/refresh",
                                "/api/auth/validate",
                                "/api/auth/revoke",

                                // RegistrationController
                                "/api/register/**",

                                // PasswordController
                                "/api/password/**",

                                // JwksController
                                "/.well-known/**"
                        ).permitAll()

                        // Endpoints administrativos: exigem authority ROLE_ADMIN (via claim "roles" do JWT).
                        // Ex.: AdminUserController -> /api/admin/users ; ClientApplicationController -> /api/admin/clients
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Demais endpoints precisam apenas estar autenticados
                        // Ex.: UserProfileController -> /api/user/me
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        // headers extras, HSTS, CSP etc.
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'"))
                .frameOptions(frame -> frame.sameOrigin())
        );

        return http.build();
    }

    /**
     * Mapeia o claim "roles" do JWT (ex.: ["ROLE_ADMIN","ROLE_USER"]) para authorities
     * do Spring Security. Como os nomes já vêm com prefixo ROLE_, o authorityPrefix é
     * vazio para não duplicar (evita "ROLE_ROLE_ADMIN"). Assim hasRole("ADMIN") funciona.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /**
     * CORS aplicado ao Spring Security a partir das propriedades cors.allowed-*.
     * Não usa "*" com allowCredentials=true (combinação inválida/insegura). Como
     * origins são explícitos (ex.: http://localhost:3000), credentials podem ser
     * habilitados. Em prod, defina cors.allowed-origins via variável de ambiente.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(splitCsv(corsAllowedOrigins));
        config.setAllowedMethods(splitCsv(corsAllowedMethods));
        config.setAllowedHeaders(splitCsv(corsAllowedHeaders));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
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

    /**
     * JwtDecoder baseado na MESMA chave pública usada pelo JwtNimbusTokenProviderAdapter.
     * Em modo dev, a chave é gerada em memória no adapter.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            RSAKey rsaJwk = jwtNimbusTokenProviderAdapter.toRsaJwk();
            RSAPublicKey publicKey = rsaJwk.toRSAPublicKey();

            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
            decoder.setJwtValidator(new JwtValidatorFactory(jwtProperties).create());
            return decoder;

        } catch (JOSEException e) {

            throw new IllegalStateException("Could not create JwtDecoder from in-memory JWK", e);
        }
    }

    // fabrica simples para montar um validator customizado de JWT
    static class JwtValidatorFactory {

        private final JwtProperties jwtProperties;

        JwtValidatorFactory(JwtProperties jwtProperties) {
            this.jwtProperties = jwtProperties;
        }

        public OAuth2TokenValidator<Jwt> create() {

            var validators = new ArrayList<OAuth2TokenValidator<Jwt>>();

            if (jwtProperties.getIssuer() != null) {
                validators.add(new JwtIssuerValidator(jwtProperties.getIssuer()));
            }

            // validador default (timestamps, exp, nbf etc.)
            validators.add(JwtValidators.createDefault());

            return new DelegatingOAuth2TokenValidator<>(validators);

        }

    }

}
