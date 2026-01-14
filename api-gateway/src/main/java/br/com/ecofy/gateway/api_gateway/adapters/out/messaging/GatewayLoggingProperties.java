package br.com.ecofy.gateway.api_gateway.adapters.out.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecofy.gateway.logging")
public record GatewayLoggingProperties(
        boolean enabled,
        String topicName
) {
    public GatewayLoggingProperties {
        if (topicName == null || topicName.isBlank()) {
            topicName = "gateway.access.log";
        }
    }
}
