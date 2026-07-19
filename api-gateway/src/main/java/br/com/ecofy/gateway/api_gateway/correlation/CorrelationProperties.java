package br.com.ecofy.gateway.api_gateway.correlation;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Configura o tamanho máximo permitido para o correlation ID.
@Validated
@ConfigurationProperties(prefix = "ecofy.gateway.correlation")
public class CorrelationProperties {

    @Min(8)
    private int maxLength = 128;

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
}
