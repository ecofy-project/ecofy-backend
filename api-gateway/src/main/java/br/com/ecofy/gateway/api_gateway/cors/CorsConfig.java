package br.com.ecofy.gateway.api_gateway.cors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Política de CORS explícita, construída a partir de {@link CorsProperties} (ECO-19).
 *
 * Usa um {@link CorsWebFilter} (WebFilter reativo) em vez de {@code globalcors} do
 * gateway, o que dá controle total e testável por profile e trata o preflight
 * {@code OPTIONS} antes do roteamento. As origens são sempre explícitas — nunca
 * um wildcard genérico. {@code exposedHeaders} inclui X-Correlation-Id para o
 * frontend. O uso de {@code allowedOriginPatterns} vs {@code allowedOrigins} é
 * escolhido conforme a necessidade de credenciais.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.getAllowedOrigins());
        config.setAllowedMethods(properties.getAllowedMethods());
        config.setAllowedHeaders(properties.getAllowedHeaders());
        config.setExposedHeaders(properties.getExposedHeaders());
        config.setAllowCredentials(properties.isAllowCredentials());
        config.setMaxAge(properties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
