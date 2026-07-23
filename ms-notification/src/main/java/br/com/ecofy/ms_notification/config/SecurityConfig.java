package br.com.ecofy.ms_notification.config;

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

// Centraliza as regras de autenticação, autorização e proteção HTTP do serviço.
@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private static final String PERMIT_ALL_PROPERTY =
            "ecofy.notification.security.permit-all";

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] NOTIFICATION_API_ENDPOINTS = {
            "/api/notification/**"
    };

    // Configura uma API stateless protegida por JWT com liberação controlada por ambiente.
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Environment env
    ) throws Exception {
        boolean devPermitAll = env.getProperty(
                PERMIT_ALL_PROPERTY,
                Boolean.class,
                false
        );

        log.info(
                "[SecurityConfig] - [securityFilterChain] -> ms-notification security (permitAll={})",
                devPermitAll
        );

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();

                    if (devPermitAll) {
                        auth.requestMatchers(NOTIFICATION_API_ENDPOINTS)
                                .permitAll();
                    } else {
                        auth.requestMatchers(NOTIFICATION_API_ENDPOINTS)
                                .authenticated();
                    }

                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new BearerTokenAuthenticationEntryPoint()
                        )
                        .accessDeniedHandler(
                                new BearerTokenAccessDeniedHandler()
                        )
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'")
                        )
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers
                                        .ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy
                                        .NO_REFERRER
                        ))
                );

        return http.build();
    }
}
