package br.com.ecofy.gateway.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "gateway.dynamic")
public class DynamicGatewayProperties {

    /**
     * Ex: "/services"
     */
    private String prefix = "/services";

    /**
     * Mapa: serviceName -> baseUri (incluindo context-path do MS se existir)
     * Ex: "ms-auth" -> "http://localhost:8081/auth"
     */
    private Map<String, URI> services = new HashMap<>();

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public Map<String, URI> getServices() { return services; }
    public void setServices(Map<String, URI> services) { this.services = services; }
}