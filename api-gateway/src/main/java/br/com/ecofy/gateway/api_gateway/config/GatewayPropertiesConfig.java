package br.com.ecofy.gateway.api_gateway.config;

import br.com.ecofy.gateway.api_gateway.adapters.out.messaging.GatewayLoggingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewayLoggingProperties.class)
public class GatewayPropertiesConfig { }