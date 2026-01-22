package br.com.ecofy.ms_insights.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] INSIGHTS_API_ENDPOINTS = {
            "/api/insights/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("[SecurityConfig] - [securityFilterChain] -> Configurando HTTP security para ms-insights (LOCAL DEV MODE)");

        http
                // API stateless
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CSRF não faz sentido em APIs stateless (sem cookies de sessão)
                .csrf(csrf -> csrf.disable())

                // CORS: habilite se o dashboard estiver em outro domínio
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // LOCAL DEV MODE (sem token) — DEIXE ATIVO para testes locais
                        .requestMatchers(INSIGHTS_API_ENDPOINTS).permitAll()

                        // PROD MODE (com token) — DESCOMENTE para exigir JWT
                        // .requestMatchers(INSIGHTS_API_ENDPOINTS).authenticated()

                        .anyRequest().authenticated()
                )

                // PROD MODE (com token) — DEIXE ATIVO para exigir JWT
                // (em local dev mode, pode comentar este bloco inteiro)
                // .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

                // Respostas corretas para 401/403 com Bearer token
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )

                // Hardening básico de headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
                        ))
                );

        return http.build();
    }

}
