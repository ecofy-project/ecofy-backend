package br.com.ecofy.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configura a integração HTTP entre o ms-auth e o ms-users.
@ConfigurationProperties(prefix = "ecofy.users-ms")
public record UsersMsProperties(
        boolean enabled,
        String baseUrl,
        String internalToken
) {}
