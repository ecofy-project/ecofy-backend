package br.com.ecofy.gateway.api_gateway.correlation;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração tipada do correlation ID (ECO-05), prefixo {@code ecofy.gateway.correlation}.
 *
 * {@code maxLength} define o tamanho máximo aceito de um valor recebido do cliente.
 * Decisão: 128 caracteres — cobre UUID (36), trace IDs de 32 hex e a maioria dos
 * formatos de correlação, sem permitir headers desproporcionais (mitiga abuso e
 * log injection por valores enormes).
 */
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
