package br.com.ecofy.ms_users.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private static final String PROP_PERMIT_ALL = "ecofy.users.security.permit-all";

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] USERS_API_ENDPOINTS = {
            "/api/users/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {
        Objects.requireNonNull(http, "http must not be null");
        Objects.requireNonNull(env, "env must not be null");

        boolean devPermitAll = Boolean.parseBoolean(env.getProperty(PROP_PERMIT_ALL, "false"));

        log.info(
                "[SecurityConfig] - [securityFilterChain] -> Configurando HTTP security para ms-users devPermitAll={}",
                devPermitAll
        );

        http
                // API stateless
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CSRF não faz sentido em APIs stateless
                .csrf(csrf -> csrf.disable())

                // CORS: habilite se o dashboard estiver em outro domínio
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();

                    if (devPermitAll) {
                        // LOCAL DEV MODE: libera API inteira para testes locais
                        auth.requestMatchers(USERS_API_ENDPOINTS).permitAll();
                        auth.anyRequest().permitAll();
                    } else {
                        // PROD MODE: exige JWT na API do ms-users
                        auth.requestMatchers(USERS_API_ENDPOINTS).authenticated();
                        auth.anyRequest().authenticated();
                    }
                })

                // Hardening básico de headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
                        ))
                );

        // ✅ IMPORTANTÍSSIMO: só configure Resource Server quando NÃO estiver em permit-all
        if (!devPermitAll) {
            http
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                            .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                    );
        }

        return http.build();
    }
}
