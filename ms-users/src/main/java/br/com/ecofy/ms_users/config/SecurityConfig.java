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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Objects;

// Centraliza as regras de autenticação, autorização e proteção HTTP do serviço.
@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private static final String PROP_PERMIT_ALL =
            "ecofy.users.security.permit-all";

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

    // Configura uma API stateless com autenticação interna e JWT controlado por ambiente.
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Environment env,
            InternalTokenAuthenticationFilter internalTokenAuthenticationFilter
    ) throws Exception {
        Objects.requireNonNull(http, "http must not be null");
        Objects.requireNonNull(env, "env must not be null");
        Objects.requireNonNull(
                internalTokenAuthenticationFilter,
                "internalTokenAuthenticationFilter must not be null"
        );

        boolean devPermitAll = Boolean.parseBoolean(
                env.getProperty(PROP_PERMIT_ALL, "false")
        );

        log.info(
                "[SecurityConfig] - [securityFilterChain] -> Configurando HTTP security para ms-users devPermitAll={}",
                devPermitAll
        );

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS)
                            .permitAll();

                    auth.requestMatchers("/internal/**")
                            .hasRole("INTERNAL");

                    if (devPermitAll) {
                        auth.requestMatchers(USERS_API_ENDPOINTS)
                                .permitAll();
                        auth.anyRequest().permitAll();
                    } else {
                        auth.requestMatchers(USERS_API_ENDPOINTS)
                                .authenticated();
                        auth.anyRequest().authenticated();
                    }
                })
                .addFilterBefore(
                        internalTokenAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
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

        if (!devPermitAll) {
            http
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
                    );
        }

        return http.build();
    }
}
