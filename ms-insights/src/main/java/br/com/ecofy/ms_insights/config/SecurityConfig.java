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

// Configura autenticação, autorização e proteção dos endpoints HTTP.
@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private static final String PERMIT_ALL_PROPERTY =
            "ecofy.insights.security.permit-all";

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

    // Configura a cadeia de segurança stateless com autenticação JWT.
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
                "[SecurityConfig] - [securityFilterChain] -> ms-insights security (permitAll={})",
                devPermitAll
        );

        http
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();

                    if (devPermitAll) {
                        auth.requestMatchers(INSIGHTS_API_ENDPOINTS)
                                .permitAll();
                    } else {
                        auth.requestMatchers(INSIGHTS_API_ENDPOINTS)
                                .authenticated();
                    }

                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new BearerTokenAuthenticationEntryPoint()
                        )
                        .accessDeniedHandler(
                                new BearerTokenAccessDeniedHandler()
                        )
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'"
                        ))
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers
                                        .ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.NO_REFERRER
                        ))
                );

        return http.build();
    }
}
