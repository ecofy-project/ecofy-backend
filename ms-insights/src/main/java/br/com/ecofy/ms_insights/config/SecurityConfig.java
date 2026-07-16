package br.com.ecofy.ms_insights.config;

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

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private static final String PERMIT_ALL_PROPERTY = "ecofy.insights.security.permit-all";

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {

        // Correção Dia 8 (item #1): antes /api/insights/** era permitAll fixo e o Resource Server JWT
        // estava comentado -> API totalmente aberta apesar do OpenAPI declarar Bearer JWT.
        // Agora: JWT sempre configurado; permit-all controlado por profile (default: exigir JWT).
        boolean devPermitAll = env.getProperty(PERMIT_ALL_PROPERTY, Boolean.class, false);

        log.info("[SecurityConfig] - [securityFilterChain] -> ms-insights security (permitAll={})", devPermitAll);

        http
                // API stateless
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CSRF não faz sentido em APIs stateless (sem cookies de sessão)
                .csrf(csrf -> csrf.disable())

                // CORS: habilite se o dashboard estiver em outro domínio
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();

                    if (devPermitAll) {
                        // dev/test/local: libera a API para facilitar testes (documentado)
                        auth.requestMatchers(INSIGHTS_API_ENDPOINTS).permitAll();
                    } else {
                        // prod: exige JWT válido nos endpoints de negócio
                        auth.requestMatchers(INSIGHTS_API_ENDPOINTS).authenticated();
                    }

                    auth.anyRequest().authenticated();
                })

                // Resource Server JWT sempre ativo (validação via JWKS do ms-auth), alinhado ao OpenAPI
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

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
