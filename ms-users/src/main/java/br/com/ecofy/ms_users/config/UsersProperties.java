package br.com.ecofy.ms_users.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ecofy.users")
public record UsersProperties(

        Topics topics,

        Idempotency idempotency,

        ExternalAuth externalAuth

) {

    public record Topics(
            String authUserCreated,
            String ecoUserEvent
    ) {}

    public record Idempotency(
            Duration ttl
    ) {}

    public record ExternalAuth(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout
    ) {}

}
