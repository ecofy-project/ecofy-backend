package br.com.ecofy.ms_insights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "ecofy.insights.clients")
public record ExternalClientsProperties(
        Categorization categorization,
        Budgeting budgeting
) {

    // Valida que os blocos de configuração de clients (categorization/budgeting) foram carregados e não são nulos.
    public ExternalClientsProperties {
        Objects.requireNonNull(categorization, "categorization must not be null");
        Objects.requireNonNull(budgeting, "budgeting must not be null");
    }

    public record Categorization(
            boolean enabled,
            String baseUrl,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        // Normaliza a baseUrl e valida que os timeouts configurados são positivos para o client de categorization.
        public Categorization {
            baseUrl = normalizeUrl(baseUrl);
            connectTimeoutMs = requirePositive(connectTimeoutMs, "categorization.connectTimeoutMs");
            readTimeoutMs = requirePositive(readTimeoutMs, "categorization.readTimeoutMs");
        }
    }

    public record Budgeting(
            boolean enabled,
            String baseUrl,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        // Normaliza a baseUrl e valida que os timeouts configurados são positivos para o client de budgeting.
        public Budgeting {
            baseUrl = normalizeUrl(baseUrl);
            connectTimeoutMs = requirePositive(connectTimeoutMs, "budgeting.connectTimeoutMs");
            readTimeoutMs = requirePositive(readTimeoutMs, "budgeting.readTimeoutMs");
        }
    }

    // Valida que um valor numérico de configuração é estritamente positivo, lançando IllegalArgumentException quando inválido.
    private static int requirePositive(int v, String field) {
        if (v <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
        return v;
    }

    // Normaliza uma URL de base removendo espaços e barras finais para facilitar a concatenação consistente de paths.
    private static String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String u = url.trim();
        // Remove trailing slash para facilitar concatenação de paths
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

}
