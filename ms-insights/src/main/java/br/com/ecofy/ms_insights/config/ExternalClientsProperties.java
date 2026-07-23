package br.com.ecofy.ms_insights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

// Centraliza as configurações dos clientes externos.
@ConfigurationProperties(prefix = "ecofy.insights.clients")
public record ExternalClientsProperties(
        Categorization categorization,
        Budgeting budgeting
) {

    public ExternalClientsProperties {
        Objects.requireNonNull(
                categorization,
                "categorization must not be null"
        );
        Objects.requireNonNull(
                budgeting,
                "budgeting must not be null"
        );
    }

    // Configura a integração com o serviço de categorização.
    public record Categorization(
            boolean enabled,
            String baseUrl,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        public Categorization {
            baseUrl = normalizeUrl(baseUrl);
            connectTimeoutMs = requirePositive(
                    connectTimeoutMs,
                    "categorization.connectTimeoutMs"
            );
            readTimeoutMs = requirePositive(
                    readTimeoutMs,
                    "categorization.readTimeoutMs"
            );
        }
    }

    // Configura a integração com o serviço de orçamentos.
    public record Budgeting(
            boolean enabled,
            String baseUrl,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        public Budgeting {
            baseUrl = normalizeUrl(baseUrl);
            connectTimeoutMs = requirePositive(
                    connectTimeoutMs,
                    "budgeting.connectTimeoutMs"
            );
            readTimeoutMs = requirePositive(
                    readTimeoutMs,
                    "budgeting.readTimeoutMs"
            );
        }
    }

    // Valida valores numéricos que devem ser positivos.
    private static int requirePositive(int v, String field) {
        if (v <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
        return v;
    }

    // Normaliza URLs para concatenação consistente de caminhos.
    private static String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
