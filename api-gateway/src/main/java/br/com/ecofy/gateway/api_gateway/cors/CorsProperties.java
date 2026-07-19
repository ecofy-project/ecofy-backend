package br.com.ecofy.gateway.api_gateway.cors;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Configura as regras de CORS conforme o ambiente da aplicação.
@ConfigurationProperties(prefix = "ecofy.gateway.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of();
    private List<String> allowedMethods = List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
    );
    private List<String> allowedHeaders = List.of(
            "Authorization",
            "Content-Type",
            "X-Correlation-Id"
    );
    private List<String> exposedHeaders = List.of("X-Correlation-Id");
    private boolean allowCredentials = false;
    private long maxAge = 3600;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}
