package br.com.ecofy.ms_users.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ecofy.users")
public record UsersProperties(

        Topics topics,

        Idempotency idempotency,

        ExternalAuth externalAuth,

        Internal internal,

        Pagination pagination

) {

    public record Topics(
            String authUserCreated,
            String ecoUserEvent
    ) {}

    // Define os limites de paginação externos, com teto obrigatório para o tamanho da página.
    public record Pagination(
            int defaultSize,
            int maxSize
    ) {
        public Pagination {
            if (defaultSize <= 0) {
                throw new IllegalArgumentException("ecofy.users.pagination.default-size must be > 0");
            }
            if (maxSize < defaultSize) {
                throw new IllegalArgumentException(
                        "ecofy.users.pagination.max-size must be >= default-size");
            }
        }
    }

    public record Idempotency(
            Duration ttl
    ) {}

    public record ExternalAuth(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout
    ) {}

    public record Internal(
            boolean enabled,
            String token
    ) {}

}
