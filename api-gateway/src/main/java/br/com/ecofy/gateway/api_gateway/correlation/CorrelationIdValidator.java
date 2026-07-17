package br.com.ecofy.gateway.api_gateway.correlation;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Valida e gera correlation IDs (ECO-05).
 *
 * Um valor recebido do cliente só é aceito quando é seguro para uso em header
 * HTTP e em logs. São rejeitados valores vazios, longos demais, com quebras de
 * linha ou com caracteres fora de um conjunto conservador (mitiga log injection
 * e header smuggling). Quando o valor é inválido, um novo ID é gerado — nunca se
 * propaga conteúdo inseguro recebido do cliente.
 */
@Component
public class CorrelationIdValidator {

    // Conjunto conservador: alfanuméricos e separadores comuns de IDs.
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final CorrelationProperties properties;

    public CorrelationIdValidator(CorrelationProperties properties) {
        this.properties = properties;
    }

    public boolean isValid(String candidate) {
        if (candidate == null) {
            return false;
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty() || trimmed.length() > properties.getMaxLength()) {
            return false;
        }
        return SAFE.matcher(trimmed).matches();
    }

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
