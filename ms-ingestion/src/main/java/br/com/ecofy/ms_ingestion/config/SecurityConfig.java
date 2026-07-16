package br.com.ecofy.ms_ingestion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    // Facilita testes locais (dev/test); em prod deve ser false para exigir JWT em /api/import/**.
    private static final String PROP_PERMIT_ALL = "ecofy.ingestion.security.permit-all";

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] INGESTION_API_ENDPOINTS = {
            "/api/import/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {
        Objects.requireNonNull(env, "env must not be null");

        boolean devPermitAll = Boolean.parseBoolean(env.getProperty(PROP_PERMIT_ALL, "false"));

        log.info(
                "[SecurityConfig] - [securityFilterChain] -> Configurando HTTP security para ms-ingestion devPermitAll={}",
                devPermitAll
        );

        http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();

                    if (devPermitAll) {
                        // DEV/TEST/LOCAL: facilita testes locais sem token.
                        auth.requestMatchers(INGESTION_API_ENDPOINTS).permitAll();
                        auth.anyRequest().permitAll();
                    } else {
                        // PROD: exige JWT em /api/import/** e demais endpoints.
                        auth.requestMatchers(INGESTION_API_ENDPOINTS).authenticated();
                        auth.anyRequest().authenticated();
                    }
                })
                // Resource Server JWT sempre disponível (usado quando não é permit-all).
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'"))
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

}
