package br.com.ecofy.ms_categorization.config;

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

// Configura a segurança HTTP e a autenticação JWT do serviço.
@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private static final String PROP_PERMIT_ALL = "ecofy.categorization.security.permit-all";

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] CATEGORIZATION_API_ENDPOINTS = {
            "/api/categorization/**"
    };

    // Configura autenticação stateless e acesso conforme o ambiente.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {
        Objects.requireNonNull(env, "env must not be null");

        boolean devPermitAll = Boolean.parseBoolean(env.getProperty(PROP_PERMIT_ALL, "false"));

        log.info("[SecurityConfig] - [securityFilterChain] -> Configurando HTTP security para ms-categorization devPermitAll={}",
                devPermitAll);

        http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();

                    if (devPermitAll) {
                        auth.requestMatchers(CATEGORIZATION_API_ENDPOINTS).permitAll();
                        auth.anyRequest().permitAll();
                    } else {
                        auth.requestMatchers(CATEGORIZATION_API_ENDPOINTS).authenticated();
                        auth.anyRequest().authenticated();
                    }
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}
