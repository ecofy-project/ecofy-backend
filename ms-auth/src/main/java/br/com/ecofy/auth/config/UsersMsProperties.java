package br.com.ecofy.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecofy.users-ms")
public record UsersMsProperties(
        boolean enabled,
        String baseUrl,
        String internalToken
) {}