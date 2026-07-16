package br.com.ecofy.ms_budgeting.config;

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

    // Facilita testes locais (dev/test); em prod deve ser false para exigir JWT em /api/budgeting/**.
    private static final String PROP_PERMIT_ALL = "ecofy.budgeting.security.permit-all";

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] BUDGETING_API_ENDPOINTS = {
            "/api/budgeting/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {
        Objects.requireNonNull(env, "env must not be null");

        boolean devPermitAll = Boolean.parseBoolean(env.getProperty(PROP_PERMIT_ALL, "false"));

        log.info("[SecurityConfig] - [securityFilterChain] -> Configurando HTTP security para ms-budgeting devPermitAll={}",
                devPermitAll);

        http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();

                    if (devPermitAll) {
                        // DEV/TEST/LOCAL: facilita testes locais dos endpoints de negócio sem token.
                        auth.requestMatchers(BUDGETING_API_ENDPOINTS).permitAll();
                    } else {
                        // PROD: exige JWT em /api/budgeting/**.
                        auth.requestMatchers(BUDGETING_API_ENDPOINTS).authenticated();
                    }

                    // Demais endpoints sempre exigem autenticação.
                    auth.anyRequest().authenticated();
                })
                // Resource Server JWT SEMPRE configurado (antes estava comentado -> nenhum JWT era validado).
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )
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
